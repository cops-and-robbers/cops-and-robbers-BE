package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.application.GameResultService;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameTerminationService {

    private final GameParticipantRepository gameParticipantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SystemEventFactory systemEventFactory;
    private final GameResultService gameResultService;

    // TODO: 추후 레디스 게임 데이터 삭제 필요
    void endGame(Game game, Team winner, GameEndReason reason) {
        GameResult result = gameResultService.recordGameResult(game, winner, reason);
        game.resetForNextRound();
        gameParticipantRepository.resetAllParticipantsForNextRound(game.getId(), ParticipantStatus.WAITING);

        SystemEvent gameEndEvent = systemEventFactory.createGameEndEvent(
                game.getId(), winner, reason, result.getId()
        );
        eventPublisher.publishEvent(gameEndEvent);

        log.info("[GameTerminationService] Game {} ended. Winner: {}, Reason: {}", game.getId(), winner, reason);
    }
}
