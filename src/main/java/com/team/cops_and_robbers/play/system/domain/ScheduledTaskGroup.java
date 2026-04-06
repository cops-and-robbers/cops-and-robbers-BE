package com.team.cops_and_robbers.play.system.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

class ScheduledTaskGroup {

    private final Map<TaskType, List<ScheduledFuture<?>>> tasks = new EnumMap<>(TaskType.class);

    void add(TaskType type, ScheduledFuture<?> future) {
        tasks.computeIfAbsent(type, k -> new ArrayList<>()).add(future);
    }

    boolean cancelByType(TaskType type) {
        List<ScheduledFuture<?>> list = tasks.remove(type);
        if (list == null || list.isEmpty()) return false;
        list.forEach(f -> f.cancel(false));
        return true;
    }

    int cancelAll() {
        int count = 0;
        for (List<ScheduledFuture<?>> list : tasks.values()) {
            list.forEach(f -> f.cancel(false));
            count += list.size();
        }
        tasks.clear();
        return count;
    }
}
