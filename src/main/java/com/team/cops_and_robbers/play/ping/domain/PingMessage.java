package com.team.cops_and_robbers.play.ping.domain;

public record PingMessage(
        Long gameId,
        PingType pingType,
        PingLocation location,
        PingSender pingSender
) {
    public static PingMessage of(Long gameId, PingType pingType, PingLocation location,
                                  Long participantId, String nickname) {
        return new PingMessage(gameId, pingType, location, new PingSender(participantId, nickname));
    }

    public record PingSender(Long participantId, String nickname) {}
}
