package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdditionalTimeService {

    static final int ADDITIONAL_TIME_SECONDS = 60;

    private final Clock clock;
    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final GameTerminationService gameTerminationService;
    private final ApplicationEventPublisher eventPublisher;
    private final SystemEventFactory systemEventFactory;

    /**
     * 종료 조건 발생 시 즉시 종료하지 않고 추가 시간을 부여한다.
     * - game을 추가 시간 상태로 전환
     * - ADDITIONAL_TIME_STARTED 이벤트 발행
     *   → 트랜잭션 커밋 후 SystemEventHandler가 처리:
     *     · TIME_OVER 스케줄 취소 (cancelGameOverSchedule)
     *     · 1분 후 평가 태스크 등록 (scheduleAdditionalTimeEvaluation)
     *     · 클라이언트에 WebSocket 알림 (systemPublisher)
     */
    @Transactional
    public void startAdditionalTime(Long gameId, GameEndReason trigger) {
        Game game = gameRepository.getByGameId(gameId);

        if (!game.isInProgress()) {
            log.info("[AdditionalTime] Game not in progress, skip. GameId: {}", gameId);
            return;
        }
        if (game.isInAdditionalTime()) {    // 추가시간 중 다시 전체가 다 잡힌 경우에는 다시 추가 시간이 시작 되면 안 된다.
            log.info("[AdditionalTime] Already in additional time, skip. GameId: {}", gameId);
            return;
        }

        game.startAdditionalTime(LocalDateTime.now(clock));

        SystemEvent event = systemEventFactory.createAdditionalTimeStartedEvent(gameId, trigger, ADDITIONAL_TIME_SECONDS);
        eventPublisher.publishEvent(event);

        log.info("[AdditionalTime] Started {}s additional time for GameId: {}, trigger: {}", ADDITIONAL_TIME_SECONDS, gameId, trigger);
    }

    /**
     * 추가 시간 종료 후 최종 판정.
     * - ALL_ARRESTED trigger: 전원 체포 유지 → 경찰 승리 / 탈옥 발생 → 게임 속행
     * - TIME_OVER trigger: 생존 도둑 있음 → 도둑 승리 / 전원 체포 → 경찰 승리
     */
    @Transactional
    public void evaluateAdditionalTime(Long gameId, GameEndReason trigger) {
        Game game = gameRepository.getByGameId(gameId);

        if (!game.isInProgress()) {
            log.info("[AdditionalTime] Game not in progress at evaluation, skip. GameId: {}", gameId);
            return;
        }

        int aliveRobbers = gameParticipantRepository.countByGameIdAndRobberStatus(gameId, ParticipantStatus.ALIVE);
        log.info("[AdditionalTime] Evaluating GameId: {}, trigger: {}, aliveRobbers: {}", gameId, trigger, aliveRobbers);

        if (aliveRobbers == 0) {
            gameTerminationService.endGame(game, Team.POLICE, GameEndReason.ALL_ARRESTED);
            return;
        }

        if (trigger == GameEndReason.ALL_ARRESTED) {
            resumeGame(game);
        } else {
            gameTerminationService.endGame(game, Team.ROBBER, GameEndReason.TIME_OVER);
        }
    }

    /**
     * ALL_ARRESTED 추가시간 후 탈옥 발생 → 게임 속행.
     * - GAME_RESUMED 이벤트 발행
     *   → 트랜잭션 커밋 후 SystemEventHandler가 처리:
     *     · 남은 시간만큼 TIME_OVER 태스크 재등록 (scheduleGameOverAfterDelay)
     *     · 클라이언트에 WebSocket 알림 (systemPublisher)
     * - 원래 종료 시각이 이미 지난 경우 → 즉시 TIME_OVER 추가시간 시작
     */
    private void resumeGame(Game game) {
        LocalDateTime pausedAt = game.getAdditionalTimeStartedAt();
        LocalDateTime currentScheduledEnd = game.getScheduledEndTime();
        game.endAdditionalTime();

        // 추가시간이 시작된 시점을 기준으로 남은 시간 계산 → 추가시간 동안 게임 freeze
        Duration remaining = Duration.between(pausedAt, currentScheduledEnd);

        if (remaining.isPositive()) {
            LocalDateTime newScheduledEnd = LocalDateTime.now(clock).plus(remaining);
            game.updateScheduledEndTime(newScheduledEnd);
            SystemEvent event = systemEventFactory.createGameResumedEvent(game.getId(), remaining.getSeconds());
            eventPublisher.publishEvent(event);
            log.info("[AdditionalTime] Resuming GameId: {} with {}s remaining. New scheduled end: {}",
                    game.getId(), remaining.getSeconds(), newScheduledEnd
            );
        } else {
            log.info("[AdditionalTime] Scheduled end already elapsed for GameId: {}, starting TIME_OVER additional time.",
                    game.getId()
            );
            startAdditionalTime(game.getId(), GameEndReason.TIME_OVER);
        }
    }
}
