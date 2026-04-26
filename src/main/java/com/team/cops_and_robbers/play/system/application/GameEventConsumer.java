package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.system.domain.GameScheduleEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.play.system.infrastructure.GameScheduleQueue;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventConsumer {

    private static final String VIRTUAL_THREAD_NAME = "game-event-consumer";

    private final GameScheduleQueue gameScheduleQueue;
    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final RobberLocationService robberLocationService;
    private final GameTerminationService gameTerminationService;
    private final SystemPublisher systemPublisher;
    private final SystemEventFactory systemEventFactory;
    private final TransactionTemplate transactionTemplate;

    private volatile Thread consumerThread;

    @PostConstruct
    public void start() {
        consumerThread = Thread.ofVirtual()
                .name(VIRTUAL_THREAD_NAME)
                .start(this::consume);
        log.info("[GameEventConsumer] Consumer started.");
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        if (consumerThread != null) {
            consumerThread.interrupt();
            consumerThread.join(Duration.ofSeconds(5));
            log.info("[GameEventConsumer] Consumer shutdown initiated.");
        }
    }

    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                GameScheduleEvent event = gameScheduleQueue.take();
                dispatch(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[GameEventConsumer] Unexpected error while processing event.", e);
            }
        }
        log.info("[GameEventConsumer] Consumer stopped gracefully.");
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

        gameScheduleQueue.enqueueNextReveal(game);
    }

    /**
     * 타임오버 게임 종료
     */
    private void executeGameOver(Long gameId) {
        gameTerminationService.endGameByTimeOver(gameId);
    }
}
