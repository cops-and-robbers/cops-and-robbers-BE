package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.play.system.infrastructure.GameScheduleQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventProducer {

    private final Clock clock;
    private final GameScheduleQueue gameScheduleQueue;
    private final GameRepository gameRepository;
    private final GameTerminationService gameTerminationService;

    /**
     * 게임 시작 시 모든 이벤트를 Delayed Queue에 등록한다.
     */
    public void scheduleAllEvents(Long gameId) {
        Game game = gameRepository.getByGameId(gameId);
        LocalDateTime now = LocalDateTime.now(clock);

        log.info("[Producer] Scheduling events for newly started game. GameId: {}", game.getId());

        gameScheduleQueue.enqueuePoliceMoveStart(game, now);
        gameScheduleQueue.enqueueFirstReveal(game, now);
        scheduleGameOver(game, now);

        log.info("[Producer] All events scheduled for GameId: {}", game.getId());
    }

    private void scheduleGameOver(Game game, LocalDateTime now) {
        if (game.isGameOverTimeReached(now)) {
            log.warn("[Producer] Time-over already reached for GameId: {}. Executing immediate termination.", game.getId());
            gameTerminationService.endGameByTimeOver(game.getId());
            return;
        }

        gameScheduleQueue.enqueueGameOver(game, now);
    }
}
