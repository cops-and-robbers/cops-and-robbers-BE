package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

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

    private final Map<Long, List<ScheduledFuture>> gameSchedules = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler;
    private final SystemPublisher systemPublisher;
    private final SystemEventFactory systemEventFactory;
    private final GameRepository gameRepository;
    private final RobberLocationService robberLocationService;

    /**
     * 게임 시작 시 모든 이벤트 예약
     */
    public void scheduleAllEvents(Long gameId) {
        Game game = gameRepository.getByGameId(gameId);
        scheduleGame(game);
    }

    /**
     * 서버 재시작 시 진행 중인 게임의 스케줄 복구
     */
    @PostConstruct
    public void recoverSchedules() {
        List<Game> inProgressGames = gameRepository.findByStatus(GameStatus.IN_PROGRESS);

        for (Game game : inProgressGames) {
            scheduleGame(game);
        }
    }

    private void scheduleGame(Game game) {
        List<ScheduledFuture> scheduledTasks = new ArrayList<>();
        LocalDateTime now = TimestampUtil.nowKstLocal();

        schedulePoliceMoveStart(scheduledTasks, game, now);
        scheduleRobberLocationReveals(scheduledTasks, game, now);

        gameSchedules.put(game.getId(), scheduledTasks);
    }

    private void schedulePoliceMoveStart(
            List<ScheduledFuture> scheduledTasks,
            Game game,
            LocalDateTime now
    ) {
        LocalDateTime policeMoveStartTime = policeMoveStartTime(game);

        register(scheduledTasks, policeMoveStartTime, now,
                () -> publishPoliceMoveStart(game.getId()));
    }

    private void scheduleRobberLocationReveals(
            List<ScheduledFuture> scheduledTasks,
            Game game,
            LocalDateTime now
    ) {
        LocalDateTime revealTime = firstRobberRevealTime(game);
        LocalDateTime gameOverTime = gameOverTime(game);

        while (revealTime.isBefore(gameOverTime)) {
            LocalDateTime finalRevealTime = revealTime;

            register(scheduledTasks, finalRevealTime, now,
                    () -> publishRobberLocationReveal(game.getId()));

            revealTime = revealTime.plusMinutes(game.getLocationRevealIntervalMinutes());
        }
    }

    private void register(
            List<ScheduledFuture> scheduledTasks,
            LocalDateTime targetTime,
            LocalDateTime now,
            Runnable task
    ) {
        if (targetTime.isAfter(now)) {
            Instant instant = TimestampUtil.toInstant(targetTime);
            ScheduledFuture scheduledTask = taskScheduler.schedule(task, instant);
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
