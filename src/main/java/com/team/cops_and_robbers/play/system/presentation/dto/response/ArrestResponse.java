package com.team.cops_and_robbers.play.system.presentation.dto.response;

import com.team.cops_and_robbers.play.system.application.dto.result.ArrestResult;

public record ArrestResponse(
        String robberNickname,
        int remainingThieves
) {
    public static ArrestResponse from(ArrestResult result) {
        return new ArrestResponse(
                result.robberNickname(),
                result.remainingThieves()
        );
    }
}
