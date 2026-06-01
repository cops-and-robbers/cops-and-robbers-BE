package com.team.cops_and_robbers.play.ping.presentation.dto;

import com.team.cops_and_robbers.play.ping.application.dto.PingCommand;
import com.team.cops_and_robbers.play.ping.domain.PingLocation;
import com.team.cops_and_robbers.play.ping.domain.PingType;

public record PingRequest(
        PingType pingType,
        PingLocation location
) {
    public PingCommand toCommand(Long gameId, Long participantId) {
        return new PingCommand(gameId, participantId, pingType, location.latitude(), location.longitude());
    }
}
