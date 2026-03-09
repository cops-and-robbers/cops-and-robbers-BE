package com.team.cops_and_robbers.game.game.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GameSettingsRequest(
        @Schema(description = "라운드 시간(분)", example = "30")
        @NotNull(message = "라운드 시간을 입력해주세요.")
        @Min(value = 10, message = "라운드 시간은 최소 10분입니다.")
        @Max(value = 180, message = "라운드 시간은 최대 3시간입니다.")
        Integer roundDurationMinutes,

        @Schema(description = "위치 공개 주기(분)", example = "5")
        @NotNull(message = "위치 공개 주기를 입력해주세요.")
        @Min(value = 5, message = "위치 공개 주기는 최소 5분입니다.")
        Integer locationRevealIntervalMinutes,

        @Schema(description = "경찰 대기 시간(분)", example = "3")
        @NotNull(message = "경찰 대기 시간을 입력해주세요.")
        @Min(value = 1, message = "경찰 대기 시간은 최소 1분입니다.")
        Integer policeWaitMinutes,

        @Schema(description = "최대 참가 인원", example = "10")
        @NotNull(message = "최대 참가 인원을 입력해주세요.")
        @Min(value = 2) @Max(value = 50)
        Integer maxParticipants
) {

}
