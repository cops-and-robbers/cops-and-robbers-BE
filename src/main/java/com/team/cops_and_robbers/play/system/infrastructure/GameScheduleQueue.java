package com.team.cops_and_robbers.play.system.infrastructure;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.play.system.domain.GameScheduleEvent;
import com.team.cops_and_robbers.play.system.domain.GameScheduleEventType;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GameScheduleQueue {

    private static final String QUEUE_NAME = "game:schedule:events";

    private final Clock clock;
    private final RBlockingQueue<GameScheduleEvent> blockingQueue; // 상태로 관리
    private final RDelayedQueue<GameScheduleEvent> delayedQueue;

    public GameScheduleQueue(Clock clock, RedissonClient redissonClient) {
        this.clock = clock;
        TypedJsonJacksonCodec codec = new TypedJsonJacksonCodec(GameScheduleEvent.class);
        this.blockingQueue = redissonClient.getBlockingQueue(QUEUE_NAME, codec);
        this.delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
    }

    public void enqueuePoliceMoveStart(Game game, LocalDateTime now) {
        long delayMs = delayMillis(now, game.getPoliceMoveStartTime());
        if (delayMs <= 0) {
            return;
        }
        enqueue(new GameScheduleEvent(game.getId(), GameScheduleEventType.POLICE_MOVE_START, game.getRoundNumber()), delayMs);
        log.info("[Queue] POLICE_MOVE_START enqueued in {}ms for GameId: {}", delayMs, game.getId());
    }

    public void enqueueFirstReveal(Game game, LocalDateTime now) {
        long delayMs = delayMillis(now, game.getFirstRobberRevealTime());
        if (delayMs <= 0) {
            return;
        }
        enqueue(new GameScheduleEvent(game.getId(), GameScheduleEventType.ROBBER_LOCATION_REVEAL, game.getRoundNumber()), delayMs);
        log.info("[Queue] First ROBBER_LOCATION_REVEAL enqueued in {}ms for GameId: {}", delayMs, game.getId());
    }

    public void enqueueNextReveal(Game game) {
        LocalDateTime nextReveal = LocalDateTime.now(clock).plusMinutes(game.getLocationRevealIntervalMinutes());

        if (game.isBeforeGameOver(nextReveal)) {
            long delayMs = Duration.ofMinutes(game.getLocationRevealIntervalMinutes()).toMillis();
            enqueue(new GameScheduleEvent(game.getId(), GameScheduleEventType.ROBBER_LOCATION_REVEAL, game.getRoundNumber()), delayMs);
            log.info("[Queue] Next ROBBER_LOCATION_REVEAL enqueued in {}ms for GameId: {}", delayMs, game.getId());
        } else {
            log.info("[Queue] No more ROBBER_LOCATION_REVEAL for GameId: {}", game.getId());
        }
    }

    public void enqueueGameOver(Game game, LocalDateTime now) {
        long delayMs = delayMillis(now, game.getGameOverTime());
        if (delayMs <= 0) {
            return;
        }
        enqueue(new GameScheduleEvent(game.getId(), GameScheduleEventType.GAME_OVER, game.getRoundNumber()), delayMs);
        log.info("[Queue] GAME_OVER enqueued in {}ms for GameId: {}", delayMs, game.getId());
    }

    public GameScheduleEvent take() throws InterruptedException {
        return blockingQueue.take();
    }

    private void enqueue(GameScheduleEvent event, long delayMs) {
        delayedQueue.offer(event, delayMs, TimeUnit.MILLISECONDS);
    }

    private long delayMillis(LocalDateTime now, LocalDateTime target) {
        return Duration.between(now, target).toMillis();
    }
}
