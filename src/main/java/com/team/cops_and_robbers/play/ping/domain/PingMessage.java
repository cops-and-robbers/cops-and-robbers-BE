package com.team.cops_and_robbers.play.ping.domain;

import com.team.cops_and_robbers.common.util.TimestampUtil;

import java.util.UUID;

public record PingMessage(
        String id,
        Long gameId,
        PingType pingType,
        PingLocation location,
        PingSender pingSender,
        String timestamp
) {
    public static PingMessage of(Long gameId, PingType pingType, PingLocation location,
                                  Long participantId, String nickname) {
        return new PingMessage(
                UUID.randomUUID().toString(),
                gameId,
                pingType,
                location,
                new PingSender(participantId, nickname),
                TimestampUtil.nowKstIso()
        );
    }

    public record PingSender(Long participantId, String nickname) {}
}