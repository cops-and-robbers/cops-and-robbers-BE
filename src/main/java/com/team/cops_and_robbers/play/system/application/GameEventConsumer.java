package com.team.cops_and_robbers.play.system.application;

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
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventConsumer {

    static final String QUEUE_NAME = "game:schedule:events";

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
                .name("game-event-consumer")
                .start(() -> consume(queue));
        log.info("[GameEventConsumer] Consumer started.");
    }

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

    private void dispatch(GameScheduleEvent event) {
        Long gameId = event.gameId();

        // 게임이 진행중이 아니라면 이벤트 스킵
        Game game = gameRepository.getByGameId(gameId);
        if (!game.isInProgress()) {
            log.info("[GameEventConsumer] Game {} is not in progress. Skipping: {}", gameId, event.type());
            return;
        }

        log.info("[GameEventConsumer] Executing {} for GameId: {}", event.type(), gameId);
        switch (event.type()) {
            case POLICE_MOVE_START     -> executePoliceMoveStart(gameId);
            case ROBBER_LOCATION_REVEAL -> executeRobberLocationReveal(gameId);
            case GAME_OVER             -> executeGameOver(gameId);
        }
    }

    /**
     * 경찰 출동: 경찰 참여자 상태를 POLICE_WAITING → ALIVE로 전환 후 이벤트 발행
     * TransactionTemplate 사용: 가상 스레드 내부 private 메서드는 Spring AOP 프록시 적용 불가
     */
    private void executePoliceMoveStart(Long gameId) {
        transactionTemplate.executeWithoutResult(status ->
                gameParticipantRepository.updateStatusByGameIdAndTeam(
                        gameId, Team.POLICE, ParticipantStatus.ALIVE)
        );
        systemPublisher.publish(systemEventFactory.createPoliceMoveStartEvent(gameId));
    }

    /**
     * 도둑 위치 공개: 현재 캐싱된 위치를 조회해 이벤트 발행
     */
    private void executeRobberLocationReveal(Long gameId) {
        List<SystemEventData.RobberLocation> locations =
                robberLocationService.getCurrentRobberLocations(gameId);
        systemPublisher.publish(systemEventFactory.createRobberLocationRevealEvent(gameId, locations));
    }

    /**
     * 타임오버 게임 종료: GameTerminationService가 @Transactional을 보장하므로 직접 위임
     */
    private void executeGameOver(Long gameId) {
        gameTerminationService.endGameByTimeOver(gameId);
    }
}
