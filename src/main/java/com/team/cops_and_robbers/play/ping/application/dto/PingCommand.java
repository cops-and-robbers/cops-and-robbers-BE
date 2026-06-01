package com.team.cops_and_robbers.play.ping.application.dto;

import com.team.cops_and_robbers.play.ping.domain.PingType;

public record PingCommand(
        Long gameId,
        Long participantId,
        PingType pingType,
        double latitude,
        double longitude
) {
}
