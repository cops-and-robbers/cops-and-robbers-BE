package com.team.cops_and_robbers.play.distance.application.dto.command;

import com.team.cops_and_robbers.play.distance.presentation.dto.DistanceUpdateRequest;

public record DistanceUpdateCommand(
        Long gameId,
        Long participantId,
        Double distanceMeters
) {
    public static DistanceUpdateCommand of(Long gameId, Long participantId, DistanceUpdateRequest request) {
        return new DistanceUpdateCommand(gameId, participantId, request.distanceMeters());
    }
}
