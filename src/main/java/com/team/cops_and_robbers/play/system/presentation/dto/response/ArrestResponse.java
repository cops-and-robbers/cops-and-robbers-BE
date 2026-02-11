package com.team.cops_and_robbers.play.system.presentation.dto.response;

import com.team.cops_and_robbers.play.system.application.dto.result.ArrestResult;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;

public record ArrestResponse(
        SystemEventData.ArrestData result
) {
    public static ArrestResponse from(ArrestResult result) {
        return new ArrestResponse(result.data());
    }
}
