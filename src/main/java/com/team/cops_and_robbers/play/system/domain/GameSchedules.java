package com.team.cops_and_robbers.play.system.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class GameSchedules {

    private final Map<Long, ScheduledTaskGroup> entries = new ConcurrentHashMap<>();

    public void register(Long gameId) {
        entries.put(gameId, new ScheduledTaskGroup());
    }

    public void addTask(Long gameId, TaskType type, ScheduledFuture<?> future) {
        ScheduledTaskGroup group = entries.get(gameId);
        if (group == null) {
            throw new IllegalStateException("No schedule entry found for gameId: " + gameId);
        }
        group.add(type, future);
    }

    public boolean cancelByType(Long gameId, TaskType type) {
        ScheduledTaskGroup group = entries.get(gameId);
        if (group == null) return false;
        return group.cancelByType(type);
    }

    public int cancelAll(Long gameId) {
        ScheduledTaskGroup group = entries.remove(gameId);
        if (group == null) return 0;
        return group.cancelAll();
    }
}
