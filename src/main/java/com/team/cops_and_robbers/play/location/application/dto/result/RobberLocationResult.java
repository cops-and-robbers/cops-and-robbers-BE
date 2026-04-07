package com.team.cops_and_robbers.play.location.application.dto.result;

import com.team.cops_and_robbers.play.system.domain.SystemEventData;

public record RobberLocationResult(
        Long participantId,
        String nickname,
        Double latitude,
        Double longitude
) {
    public static RobberLocationResult from(SystemEventData.RobberLocation location) {
        return new RobberLocationResult(
                location.participantId(),
                location.nickname(),
                location.latitude(),
                location.longitude()
        );
    }
}
