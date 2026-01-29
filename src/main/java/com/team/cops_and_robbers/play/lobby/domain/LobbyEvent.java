package com.team.cops_and_robbers.play.lobby.domain;

import com.team.cops_and_robbers.common.util.TimestampUtil;

import java.util.UUID;

public record LobbyEvent(
        String eventId,
        Long gameId,
        LobbyEventType type,
        String timestamp,
        LobbyEventData data
) {
    public static LobbyEvent of(Long gameId, LobbyEventType type, LobbyEventData data) {
        return new LobbyEvent(
                UUID.randomUUID().toString(),
                gameId,
                type,
                TimestampUtil.nowKstIso(),
                data
        );
    }
}
