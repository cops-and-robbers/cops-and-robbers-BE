package com.team.cops_and_robbers.play.radio.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Grafana 웹훅 페이로드 중 알림 상태만 받는다 (firing | resolved)
 */
public record RadioOverloadRequest(
        @NotBlank String status
) {

    private static final String FIRING = "firing";

    public boolean isFiring() {
        return FIRING.equals(status);
    }
}
