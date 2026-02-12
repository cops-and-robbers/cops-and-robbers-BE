package com.team.cops_and_robbers.play.system.application.dto.result;

import com.team.cops_and_robbers.play.system.domain.SystemEventData;

public record ArrestResult(
        String robberNickname,
        int remainingThieves
) {
    public static ArrestResult from(SystemEventData.ArrestData data) {
        return new ArrestResult(
                data.robber().nickname(),
                data.remainingThieves()
        );
    }
}
