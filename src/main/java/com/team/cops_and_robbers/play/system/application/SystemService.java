package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.CatchOutcome;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameRulePolicy;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.application.GameResultService;
import com.team.cops_and_robbers.play.system.application.dto.command.ArrestCommand;
import com.team.cops_and_robbers.play.system.application.dto.command.EscapeCommand;
import com.team.cops_and_robbers.play.system.application.dto.command.ForceArrestCommand;
import com.team.cops_and_robbers.play.system.application.dto.result.ArrestResult;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;

    private final ApplicationEventPublisher eventPublisher;
    private final SystemEventFactory systemEventFactory;
    private final GameTerminationService gameTerminationService;
    private final GameResultService gameResultService;
    private final GameRulePolicy gameRulePolicy;

    /**
     * 경찰이 도둑을 체포합니다.
     * - 남은 도둑이 없을 경우 게임 종료 이벤트가 추가로 발생합니다.
     */
    @Transactional
    public ArrestResult arrestRunner(ArrestCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());
        GameParticipant chaser = gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.chaserUserId());
        GameParticipant runner = gameParticipantRepository.getParticipantById(command.runnerParticipantId());

        validateArrest(game, chaser, runner);

        if (game.rules().catchOutcome() == CatchOutcome.CAPTURE) {
            runner.updateStatus(ParticipantStatus.JAILED);
        }
        game.incrementArrestCount();
        gameResultService.recordArrest(game.getId(), command.chaserUserId());
        gameResultService.recordArrested(game.getId(), runner.getUser().getId());

        int remainingRunners = gameParticipantRepository.countByGameIdAndRunnerStatus(
                game.getId(), ParticipantStatus.ALIVE
        );

        SystemEvent event = systemEventFactory.createArrestEvent(
                game.getId(), chaser, runner, remainingRunners
        );
        eventPublisher.publishEvent(event);

        if (game.rules().endsWhenAllRunnersArrested() && remainingRunners == 0) {
            gameTerminationService.endGameByAllArrested(game.getId());
        }

        return ArrestResult.from((SystemEventData.ArrestData) event.data());
    }

    @Transactional
    public ArrestResult forceArrestRunner(ForceArrestCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());
        GameParticipant runner = gameParticipantRepository.getByGameIdAndUserId(
                command.gameId(), command.runnerUserId()
        );

        validateForceArrest(game, runner);

        if (game.rules().catchOutcome() == CatchOutcome.CAPTURE) {
            runner.updateStatus(ParticipantStatus.JAILED);
        }
        game.incrementArrestCount();
        gameResultService.recordArrested(game.getId(), runner.getUser().getId());

        int remainingRunners = gameParticipantRepository.countByGameIdAndRunnerStatus(
                game.getId(), ParticipantStatus.ALIVE
        );

        SystemEvent event = systemEventFactory.createForceArrestEvent(game.getId(), runner, remainingRunners);
        eventPublisher.publishEvent(event);

        if (game.rules().endsWhenAllRunnersArrested() && remainingRunners == 0) {
            gameTerminationService.endGameByAllArrested(game.getId());
        }

        return ArrestResult.from((SystemEventData.ArrestData) event.data());
    }

    /**
     * 수감된 도둑이 탈옥합니다.
     */
    @Transactional
    public void escapeFromPrison(EscapeCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());
        GameParticipant escapedRunner = gameParticipantRepository.getByGameIdAndUserId(
                command.gameId(), command.escapedRunnerUserId()
        );

        validateEscape(game, escapedRunner);

        escapedRunner.updateStatus(ParticipantStatus.ALIVE);

        int remainingRunners = gameParticipantRepository.countByGameIdAndRunnerStatus(
                command.gameId(), ParticipantStatus.ALIVE
        );

        SystemEvent event = systemEventFactory.createEscapeEvent(command.gameId(), escapedRunner, remainingRunners);
        eventPublisher.publishEvent(event);
    }

    private void validateArrest(Game game, GameParticipant chaser, GameParticipant runner) {
        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }
        if (!chaser.isChaser()) {
            throw new ApplicationException(GameParticipantException.ONLY_CHASER_CAN_ARREST);
        }
        if (chaser.isChaserWaiting()) {
            throw new ApplicationException(GameParticipantException.CHASER_WAITING_TIME);
        }
        if (!runner.isRunner()) {
            throw new ApplicationException(GameParticipantException.ONLY_RUNNER_CAN_BE_ARRESTED);
        }
        if (!runner.isInGame(game.getId())) {
            throw new ApplicationException(GameParticipantException.PARTICIPANT_GAME_MISMATCH);
        }
        if (runner.isJailed()) {
            throw new ApplicationException(GameParticipantException.ALREADY_ARRESTED);
        }
    }

    private void validateForceArrest(Game game, GameParticipant runner) {
        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }
        gameRulePolicy.validateForceArrest(game);
        if (!runner.isRunner()) {
            throw new ApplicationException(GameParticipantException.ONLY_RUNNER_CAN_BE_ARRESTED);
        }
        if (runner.isJailed()) {
            throw new ApplicationException(GameParticipantException.ALREADY_ARRESTED);
        }
    }

    private void validateEscape(Game game, GameParticipant escapedRunner) {
        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }
        gameRulePolicy.validateEscape(game);
        if (!escapedRunner.isJailed()) {
            throw new ApplicationException(GameParticipantException.NOT_JAILED);
        }
    }
}
