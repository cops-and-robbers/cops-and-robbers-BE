package com.team.cops_and_robbers.play.location.application.dto.command;

import com.team.cops_and_robbers.play.location.presentation.dto.LocationUpdateRequest;

public record LocationUpdateCommand(
        Long gameId,
        Long participantId,
        Double latitude,
        Double longitude
) {
    public static LocationUpdateCommand of(Long gameId, Long participantId, LocationUpdateRequest request) {
        return new LocationUpdateCommand(gameId, participantId, request.latitude(), request.longitude());
    }
}