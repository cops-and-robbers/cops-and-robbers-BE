package com.team.cops_and_robbers.play.system.domain;

import com.team.cops_and_robbers.common.util.TimestampUtil;

import java.util.UUID;

public record SystemEvent(
        String eventId,
        Long gameId,
        SystemEventType type,
        String timestamp,
        SystemEventData data
) {
    public static SystemEvent of(Long gameId, SystemEventType type, SystemEventData data) {
        return new SystemEvent(
                UUID.randomUUID().toString(),
                gameId,
                type,
                TimestampUtil.nowKstIso(),
                data
        );
    }
}
