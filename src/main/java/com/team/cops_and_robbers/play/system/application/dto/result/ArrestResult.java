package com.team.cops_and_robbers.play.system.application.dto.result;

import com.team.cops_and_robbers.play.system.domain.SystemEventData;

public record ArrestResult(
        SystemEventData.ArrestData data
) {
    public static ArrestResult from(SystemEventData.ArrestData data) {
        return new ArrestResult(data);
    }
}
