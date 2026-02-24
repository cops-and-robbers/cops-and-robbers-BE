package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameSchedulerService {

    private final Map<Long, List<ScheduledFuture<?>>> gameSchedules = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler;
    private final SystemPublisher systemPublisher;
    private final SystemEventFactory systemEventFactory;
    private final GameRepository gameRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final RobberLocationService robberLocationService;
    private final GameTerminationService gameTerminationService;
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
            scheduleGame(game);
        }
    }

    /**
     * 하나의 게임에 대해 필요한 시스템 이벤트 스케줄을 등록한다.
     * - 경찰 이동 시작 이벤트
     * - 도둑 위치 공개 이벤트
     * - 타임오버로 인한 게임 종료 이벤트
     * 서버 재시작 시에도 동일한 로직으로 스케줄을 복구한다.
     */
    private void scheduleGame(Game game) {
        cancelSchedule(game.getId());

        List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();
        LocalDateTime now = TimestampUtil.nowKstLocal();

        schedulePoliceMoveStart(scheduledTasks, game, now);
        scheduleRobberLocationReveals(scheduledTasks, game, now);
        scheduleGameOver(scheduledTasks, game, now);

        gameSchedules.put(game.getId(), scheduledTasks);
        log.info("[Scheduler] Successfully scheduled all events for GameId: {}", game.getId());
    }

    private void schedulePoliceMoveStart(
            List<ScheduledFuture<?>> scheduledTasks,
            Game game,
            LocalDateTime now
    ) {
        LocalDateTime policeMoveStartTime = policeMoveStartTime(game);

        register(scheduledTasks, policeMoveStartTime, now,
                () -> {
                    log.info("[Scheduler] Executing police-move-start task! GameId: {}", game.getId());
                    transactionTemplate.executeWithoutResult(status ->
                            gameParticipantRepository.updateStatusByGameIdAndTeam(game.getId(), Team.POLICE, ParticipantStatus.ALIVE)
                    );
                    publishPoliceMoveStart(game.getId());
                });
    }

    private void scheduleRobberLocationReveals(
            List<ScheduledFuture<?>> scheduledTasks,
            Game game,
            LocalDateTime now
    ) {
        LocalDateTime revealTime = firstRobberRevealTime(game);
        LocalDateTime gameOverTime = gameOverTime(game);

        while (revealTime.isBefore(gameOverTime)) {
            LocalDateTime finalRevealTime = revealTime;

            register(scheduledTasks, finalRevealTime, now,
                    () -> {
                        log.info("[Scheduler] Executing robber-location-reveal task! GameId: {}", game.getId());
                        publishRobberLocationReveal(game.getId());
                    });

            revealTime = revealTime.plusMinutes(game.getLocationRevealIntervalMinutes());
        }
    }

    /**
     * 타임 오버로 인한 게임 종료
     */
    private void scheduleGameOver(
            List<ScheduledFuture<?>> scheduledTasks,
            Game game,
            LocalDateTime now
    ) {
        LocalDateTime targetTime = gameOverTime(game);

        if (!targetTime.isAfter(now)) {
            log.warn("[Scheduler] Time-over already reached for GameId: {}. Executing immediate game termination.", game.getId());
            gameTerminationService.endGameByTimeOver(game.getId());
            return;
        }

        register(scheduledTasks, targetTime, now,
                () -> {
                    log.info("[Scheduler] Executing time-out game-over task! GameId: {}", game.getId());
                    gameTerminationService.endGameByTimeOver(game.getId());
                });
    }

    /**
     * 게임 종료/시작 시에 게임에 남아 있는 스케줄러를 모두 취소한다.
     */
    public void cancelSchedule(Long gameId) {
        List<ScheduledFuture<?>> tasks = gameSchedules.remove(gameId);

        if (tasks != null) {
            for (ScheduledFuture<?> task : tasks) {
                task.cancel(false);
            }
            log.info("[Scheduler] Cancelled all {} scheduled tasks for GameId {}.", tasks.size(), gameId);
        }
    }

    private void register(
            List<ScheduledFuture<?>> scheduledTasks,
            LocalDateTime targetTime,
            LocalDateTime now,
            Runnable task
    ) {
        if (targetTime.isAfter(now)) {
            Instant instant = TimestampUtil.toInstant(targetTime);
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(task, instant);
            scheduledTasks.add(scheduledTask);
        }
    }

    private LocalDateTime policeMoveStartTime(Game game) {
        return game.getStartedAt()
                .plusMinutes(game.getPoliceWaitMinutes());
    }

    private LocalDateTime gameOverTime(Game game) {
        return game.getStartedAt()
                .plusMinutes(game.getRoundDurationMinutes());
    }

    private LocalDateTime firstRobberRevealTime(Game game) {
        return policeMoveStartTime(game)
                .plusMinutes(game.getLocationRevealIntervalMinutes());
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
