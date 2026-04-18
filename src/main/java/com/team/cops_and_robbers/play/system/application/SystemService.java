package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.system.application.dto.command.ArrestCommand;
import com.team.cops_and_robbers.play.system.application.dto.command.EscapeCommand;
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

    /**
     * 경찰이 도둑을 체포합니다.
     * - 남은 도둑이 없을 경우 게임 종료 이벤트가 추가로 발생합니다.
     */
    @Transactional
    public ArrestResult arrestRobber(ArrestCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());
        GameParticipant police = gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.policeUserId());
        GameParticipant robber = gameParticipantRepository.getParticipantById(command.robberParticipantId());

        validateArrest(game, police, robber);

        robber.updateStatus(ParticipantStatus.JAILED);
        game.incrementArrestCount();

        int remainingThieves = gameParticipantRepository.countByGameIdAndRobberStatus(
                game.getId(), ParticipantStatus.ALIVE
        );

        SystemEvent event = systemEventFactory.createArrestEvent(
                game.getId(), police, robber, remainingThieves
        );
        eventPublisher.publishEvent(event);

        if (remainingThieves == 0) {
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
        GameParticipant escapedThief = gameParticipantRepository.getByGameIdAndUserId(
                command.gameId(), command.escapedThiefUserId()
        );

        validateEscape(game, escapedThief);

        escapedThief.updateStatus(ParticipantStatus.ALIVE);

        int remainingThieves = gameParticipantRepository.countByGameIdAndRobberStatus(
                command.gameId(), ParticipantStatus.ALIVE
        );

        SystemEvent event = systemEventFactory.createEscapeEvent(command.gameId(), escapedThief, remainingThieves);
        eventPublisher.publishEvent(event);
    }

    private void validateArrest(Game game, GameParticipant police, GameParticipant robber) {
        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }
        if (!police.isPolice()) {
            throw new ApplicationException(GameParticipantException.ONLY_POLICE_CAN_ARREST);
        }
        if (police.isPoliceWaiting()) {
            throw new ApplicationException(GameParticipantException.POLICE_WAITING_TIME);
        }
        if (!robber.isRobber()) {
            throw new ApplicationException(GameParticipantException.ONLY_ROBBER_CAN_BE_ARRESTED);
        }
        if (!robber.isInGame(game.getId())) {
            throw new ApplicationException(GameParticipantException.PARTICIPANT_GAME_MISMATCH);
        }
        if (robber.isJailed()) {
            throw new ApplicationException(GameParticipantException.ALREADY_ARRESTED);
        }
    }

    private void validateEscape(Game game, GameParticipant escapedThief) {
        if (!game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }
        if (!escapedThief.isJailed()) {
            throw new ApplicationException(GameParticipantException.NOT_JAILED);
        }
    }
}
