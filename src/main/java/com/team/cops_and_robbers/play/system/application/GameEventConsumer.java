package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.constant.RedisQueue;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.system.domain.GameScheduleEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.team.cops_and_robbers.play.system.domain.GameScheduleEventType;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventConsumer {

    private static final String QUEUE_NAME = RedisQueue.GAME_SCHEDULE.getName();
    private static final String VIRTUAL_THREAD_NAME = "game:schedule:events";

    private final Clock clock;
    private final RedissonClient redissonClient;
    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final RobberLocationService robberLocationService;
    private final GameTerminationService gameTerminationService;
    private final SystemPublisher systemPublisher;
    private final SystemEventFactory systemEventFactory;
    private final TransactionTemplate transactionTemplate;

    @PostConstruct
    public void start() {
        RBlockingQueue<GameScheduleEvent> queue = redissonClient.getBlockingQueue(QUEUE_NAME);
        Thread.ofVirtual()
                .name(VIRTUAL_THREAD_NAME)
                .start(() -> consume(queue));
        log.info("[GameEventConsumer] Consumer started.");
    }

    /**
     * 루프를 돌며 이벤트를 큐에서 꺼내 실행
     */
    private void consume(RBlockingQueue<GameScheduleEvent> queue) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                GameScheduleEvent event = queue.take();
                dispatch(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("[GameEventConsumer] Unexpected error while processing event.", e);
            }
        }
    }

    /**
     * 이벤트를 검증하고 적절한 이벤트 메서드를 디스패치
     */
    private void dispatch(GameScheduleEvent event) {
        Long gameId = event.gameId();
        Game game = gameRepository.getByGameId(gameId);

        // 1. 게임이 진행 중이 아니면 스킵
        if (!game.isInProgress()) {
            log.info("[GameEventConsumer] Game {} is not in progress. Skipping: {}", gameId, event.type());
            return;
        }

        // 2. 이전 라운드 이벤트면 스킵
        if (!game.isSameRound(event.roundNumber())) {
            log.info("[GameEventConsumer] Stale event for Game {} (event round={}, current round={}). Skipping: {}",
                    gameId, event.roundNumber(), game.getRoundNumber(), event.type());
            return;
        }

        log.info("[GameEventConsumer] Executing {} for GameId: {}", event.type(), gameId);
        switch (event.type()) {
            case POLICE_MOVE_START      -> executePoliceMoveStart(gameId);
            case ROBBER_LOCATION_REVEAL -> executeRobberLocationReveal(game);
            case GAME_OVER              -> executeGameOver(gameId);
        }
    }

    /**
     * 경찰 출동: 경찰 참여자 상태를 POLICE_WAITING → ALIVE로 전환 후 이벤트 발행
     */
    private void executePoliceMoveStart(Long gameId) {
        transactionTemplate.executeWithoutResult(status ->
                gameParticipantRepository.updateStatusByGameIdAndTeam(
                        gameId, Team.POLICE, ParticipantStatus.ALIVE)
        );
        systemPublisher.publish(systemEventFactory.createPoliceMoveStartEvent(gameId));
    }

    /**
     * 도둑 위치 공개: 위치 발행 후 다음 공개 이벤트 재등록
     */
    private void executeRobberLocationReveal(Game game) {
        List<SystemEventData.RobberLocation> locations =
                robberLocationService.getCurrentRobberLocations(game.getId());
        systemPublisher.publish(systemEventFactory.createRobberLocationRevealEvent(game.getId(), locations));

        LocalDateTime nextReveal = LocalDateTime.now(clock).plusMinutes(game.getLocationRevealIntervalMinutes());
        LocalDateTime gameOverTime = game.getStartedAt().plusMinutes(game.getRoundDurationMinutes());

        if (nextReveal.isBefore(gameOverTime)) {
            long delayMs = Duration.ofMinutes(game.getLocationRevealIntervalMinutes()).toMillis();
            RBlockingQueue<GameScheduleEvent> queue = redissonClient.getBlockingQueue(QUEUE_NAME);
            RDelayedQueue<GameScheduleEvent> delayedQueue = redissonClient.getDelayedQueue(queue);
            delayedQueue.offer(
                    new GameScheduleEvent(game.getId(), GameScheduleEventType.ROBBER_LOCATION_REVEAL, game.getRoundNumber()),
                    delayMs, TimeUnit.MILLISECONDS
            );
            log.info("[GameEventConsumer] Next ROBBER_LOCATION_REVEAL re-enqueued in {}ms for GameId: {}", delayMs, game.getId());
        } else {
            log.info("[GameEventConsumer] No more ROBBER_LOCATION_REVEAL for GameId: {}", game.getId());
        }
    }

    /**
     * 타임오버 게임 종료
     */
    private void executeGameOver(Long gameId) {
        gameTerminationService.endGameByTimeOver(gameId);
    }
}
