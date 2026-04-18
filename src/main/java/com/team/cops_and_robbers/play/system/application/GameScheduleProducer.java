package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.play.system.domain.GameScheduleEvent;
import com.team.cops_and_robbers.play.system.domain.GameScheduleEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameScheduleProducer {

    private final Clock clock;
    private final RedissonClient redissonClient;
    private final GameRepository gameRepository;
    private final GameTerminationService gameTerminationService;

    /**
     * 게임 시작 시 모든 이벤트를 Delayed Queue에 등록한다.
     * 서버 재시작 시에는 호출하지 않는다 — Redis가 이벤트를 영속화하므로
     * GameEventConsumer가 재시작 후 그대로 이어받는다.
     */
    public void scheduleAllEvents(Long gameId) {
        Game game = gameRepository.getByGameId(gameId);
        log.info("[Producer] Scheduling events for newly started game. GameId: {}", game.getId());

        RBlockingQueue<GameScheduleEvent> queue = redissonClient.getBlockingQueue(GameEventConsumer.QUEUE_NAME);
        RDelayedQueue<GameScheduleEvent> delayedQueue = redissonClient.getDelayedQueue(queue);

        LocalDateTime now = LocalDateTime.now(clock);

        schedulePoliceMoveStart(delayedQueue, game, now);
        scheduleRobberLocationReveals(delayedQueue, game, now);
        scheduleGameOver(delayedQueue, game, now);

        log.info("[Producer] All events scheduled for GameId: {}", game.getId());
    }

    private void schedulePoliceMoveStart(RDelayedQueue<GameScheduleEvent> delayedQueue, Game game, LocalDateTime now) {
        long delayMs = delayMillis(now, policeMoveStartTime(game));
        if (delayMs <= 0) {
            return;
        }
        delayedQueue.offer(
                new GameScheduleEvent(game.getId(), GameScheduleEventType.POLICE_MOVE_START),
                delayMs, TimeUnit.MILLISECONDS
        );
        log.info("[Producer] POLICE_MOVE_START enqueued in {}ms for GameId: {}", delayMs, game.getId());
    }

    private void scheduleRobberLocationReveals(RDelayedQueue<GameScheduleEvent> delayedQueue, Game game, LocalDateTime now) {
        LocalDateTime revealTime = firstRobberRevealTime(game);
        LocalDateTime gameOverTime = gameOverTime(game);
        int count = 0;

        while (revealTime.isBefore(gameOverTime)) {
            long delayMs = delayMillis(now, revealTime);
            if (delayMs > 0) {
                delayedQueue.offer(
                        new GameScheduleEvent(game.getId(), GameScheduleEventType.ROBBER_LOCATION_REVEAL),
                        delayMs, TimeUnit.MILLISECONDS
                );
                count++;
            }
            revealTime = revealTime.plusMinutes(game.getLocationRevealIntervalMinutes());
        }
        log.info("[Producer] {} ROBBER_LOCATION_REVEAL event(s) enqueued for GameId: {}", count, game.getId());
    }

    private void scheduleGameOver(RDelayedQueue<GameScheduleEvent> delayedQueue, Game game, LocalDateTime now) {
        long delayMs = delayMillis(now, gameOverTime(game));

        if (delayMs <= 0) {
            log.warn("[Producer] Time-over already reached for GameId: {}. Executing immediate game termination.", game.getId());
            gameTerminationService.endGameByTimeOver(game.getId());
            return;
        }

        delayedQueue.offer(
                new GameScheduleEvent(game.getId(), GameScheduleEventType.GAME_OVER),
                delayMs, TimeUnit.MILLISECONDS
        );
        log.info("[Producer] GAME_OVER enqueued in {}ms for GameId: {}", delayMs, game.getId());
    }

    private long delayMillis(LocalDateTime now, LocalDateTime target) {
        return Duration.between(now, target).toMillis();
    }

    private LocalDateTime policeMoveStartTime(Game game) {
        return game.getStartedAt().plusMinutes(game.getPoliceWaitMinutes());
    }

    private LocalDateTime gameOverTime(Game game) {
        return game.getStartedAt().plusMinutes(game.getRoundDurationMinutes());
    }

    private LocalDateTime firstRobberRevealTime(Game game) {
        return policeMoveStartTime(game).plusMinutes(game.getLocationRevealIntervalMinutes());
    }
}