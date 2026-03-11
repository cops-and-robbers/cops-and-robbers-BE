package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.system.domain.GameSchedules;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.play.system.domain.TaskType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameSchedulerService {

    private final GameSchedules gameSchedules = new GameSchedules();

    private final Clock clock;
    private final TaskScheduler taskScheduler;
    private final SystemPublisher systemPublisher;
    private final SystemEventFactory systemEventFactory;
    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final RobberLocationService robberLocationService;
    private final AdditionalTimeService additionalTimeService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 게임 시작 시 모든 이벤트 예약
     */
    public void scheduleAllEvents(Long gameId) {
        Game game = gameRepository.getByGameId(gameId);
        log.info("[Scheduler] Scheduling events for newly started game. GameId: {}", game.getId());
        scheduleGame(game);
    }

    /**
     * 서버 재시작 시 진행 중인 게임의 스케줄 복구
     */
    @PostConstruct
    public void recoverSchedules() {
        List<Game> inProgressGames = gameRepository.findByStatus(GameStatus.IN_PROGRESS);
        log.info("[Scheduler] Recovering schedules for {} in-progress game(s).", inProgressGames.size());
        for (Game game : inProgressGames) {
            if (game.isInAdditionalTime()) {
                recoverAdditionalTimeGame(game);
            } else {
                scheduleGame(game);
            }
        }
    }

    /**
     * 추가 시간 진행 중에 서버가 재시작된 경우, 즉시 평가를 실행한다.
     * (이미 1분이 경과했을 수 있으므로 즉시 실행)
     */
    private void recoverAdditionalTimeGame(Game game) {
        log.info("[Scheduler] Recovering additional-time game. Executing immediate evaluation for GameId: {}", game.getId());
        gameSchedules.register(game.getId());

        // additionalTimeStartedAt >= scheduledEndTime 이면 TIME_OVER가 추가시간을 트리거한 것
        GameEndReason trigger = !game.getAdditionalTimeStartedAt().isBefore(game.getScheduledEndTime())
                ? GameEndReason.TIME_OVER
                : GameEndReason.ALL_ARRESTED;

        transactionTemplate.executeWithoutResult(status ->
                additionalTimeService.evaluateAdditionalTime(game.getId(), trigger)
        );
    }

    /**
     * 하나의 게임에 대해 필요한 시스템 이벤트 스케줄을 등록한다.
     */
    private void scheduleGame(Game game) {
        cancelSchedule(game.getId());

        LocalDateTime now = LocalDateTime.now(clock);

        gameSchedules.register(game.getId());
        schedulePoliceMoveStart(game, now);
        scheduleRobberLocationReveals(game, now);
        scheduleGameOver(game, now);

        log.info("[Scheduler] Successfully scheduled all events for GameId: {}", game.getId());
    }

    private void schedulePoliceMoveStart(Game game, LocalDateTime now) {
        LocalDateTime policeMoveStartTime = policeMoveStartTime(game);

        scheduleTask(game.getId(), TaskType.POLICE_MOVE_START, policeMoveStartTime, now,
                () -> {
                    log.info("[Scheduler] Executing police-move-start task! GameId: {}", game.getId());
                    transactionTemplate.executeWithoutResult(status ->
                            gameParticipantRepository.updateStatusByGameIdAndTeam(game.getId(), Team.POLICE, ParticipantStatus.ALIVE)
                    );
                    publishPoliceMoveStart(game.getId());
                });
    }

    private void scheduleRobberLocationReveals(Game game, LocalDateTime now) {
        LocalDateTime revealTime = firstRobberRevealTime(game);
        LocalDateTime gameOverTime = gameOverTime(game);

        while (revealTime.isBefore(gameOverTime)) {
            LocalDateTime finalRevealTime = revealTime;

            scheduleTask(game.getId(), TaskType.LOCATION_REVEAL, finalRevealTime, now,
                    () -> {
                        log.info("[Scheduler] Executing robber-location-reveal task! GameId: {}", game.getId());
                        publishRobberLocationReveal(game.getId());
                    });

            revealTime = revealTime.plusMinutes(game.getLocationRevealIntervalMinutes());
        }
    }

    /**
     * 타임 오버 태스크 등록.
     * 시간이 되면 즉시 종료하지 않고 AdditionalTimeService를 통해 추가 시간 플로우로 진입한다.
     */
    private void scheduleGameOver(Game game, LocalDateTime now) {
        LocalDateTime targetTime = gameOverTime(game);

        if (!targetTime.isAfter(now)) {
            log.warn("[Scheduler] Time-over already reached for GameId: {}. Triggering additional time immediately.", game.getId());
            additionalTimeService.startAdditionalTime(game.getId(), GameEndReason.TIME_OVER);
            return;
        }

        Instant instant = targetTime.atZone(clock.getZone()).toInstant();
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    log.info("[Scheduler] Executing time-out task! GameId: {}", game.getId());
                    additionalTimeService.startAdditionalTime(game.getId(), GameEndReason.TIME_OVER);
                },
                instant
        );
        gameSchedules.addTask(game.getId(), TaskType.GAME_OVER, future);
    }

    /**
     * TIME_OVER 태스크만 취소한다. (추가 시간 시작 시 게임 시간 정지용)
     * SystemEventHandler가 ADDITIONAL_TIME_STARTED 이벤트 처리 후 호출한다.
     */
    public void cancelGameOverSchedule(Long gameId) {
        if (gameSchedules.cancelByType(gameId, TaskType.GAME_OVER)) {
            log.info("[Scheduler] Cancelled game-over schedule for GameId: {}", gameId);
        }
    }

    /**
     * 추가 시간 이후 게임 속행 시, 남은 시간만큼 TIME_OVER 태스크를 재등록한다.
     * SystemEventHandler가 GAME_RESUMED 이벤트 처리 후 호출한다.
     */
    public void scheduleGameOverAfterAdditionalTime(Long gameId, long delaySeconds) {
        Instant instant = Instant.now(clock).plusSeconds(delaySeconds);
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    log.info("[Scheduler] Executing resumed time-out task! GameId: {}", gameId);
                    additionalTimeService.startAdditionalTime(gameId, GameEndReason.TIME_OVER);
                },
                instant
        );
        gameSchedules.addTask(gameId, TaskType.GAME_OVER, future);
        log.info("[Scheduler] Scheduled game-over after {}s for GameId: {}", delaySeconds, gameId);
    }

    /**
     * 추가 시간 종료 후 평가 태스크를 등록한다.
     * SystemEventHandler가 ADDITIONAL_TIME_STARTED 이벤트 처리 후 호출한다.
     */
    public void scheduleAdditionalTimeEvaluation(Long gameId, GameEndReason trigger) {
        Instant instant = Instant.now(clock).plusSeconds(AdditionalTimeService.ADDITIONAL_TIME_SECONDS);
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    log.info("[Scheduler] Executing additional-time evaluation task! GameId: {}", gameId);
                    additionalTimeService.evaluateAdditionalTime(gameId, trigger);
                },
                instant
        );
        gameSchedules.addTask(gameId, TaskType.ADDITIONAL_TIME_EVALUATION, future);
        log.info("[Scheduler] Scheduled additional-time evaluation after {}s for GameId: {}", AdditionalTimeService.ADDITIONAL_TIME_SECONDS, gameId);
    }

    /**
     * 게임 종료/시작 시에 게임에 남아 있는 스케줄러를 모두 취소한다.
     */
    public void cancelSchedule(Long gameId) {
        int count = gameSchedules.cancelAll(gameId);
        if (count > 0) {
            log.info("[Scheduler] Cancelled all {} scheduled tasks for GameId {}.", count, gameId);
        }
    }

    private void scheduleTask(Long gameId, TaskType type, LocalDateTime targetTime,
                              LocalDateTime now, Runnable task) {
        if (targetTime.isAfter(now)) {
            Instant instant = targetTime.atZone(clock.getZone()).toInstant();
            ScheduledFuture<?> future = taskScheduler.schedule(task, instant);
            gameSchedules.addTask(gameId, type, future);
        }
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

    private void publishPoliceMoveStart(Long gameId) {
        SystemEvent event = systemEventFactory.createPoliceMoveStartEvent(gameId);
        systemPublisher.publish(event);
    }

    private void publishRobberLocationReveal(Long gameId) {
        List<SystemEventData.RobberLocation> locations = robberLocationService.getCurrentRobberLocations(gameId);
        SystemEvent event = systemEventFactory.createRobberLocationRevealEvent(gameId, locations);
        systemPublisher.publish(event);
    }
}
