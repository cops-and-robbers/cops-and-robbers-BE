package com.team.cops_and_robbers.auth.presentation.dto.response;

import com.team.cops_and_robbers.auth.application.dto.result.WithdrawalPreviewResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record WithdrawalPreviewResponse(
        @Schema(description = "닉네임", example = "재빠른고양이3721")
        String nickname,
        @Schema(description = "대기 중이거나 진행 중인 게임 참여 여부. true면 탈퇴할 수 없습니다.", example = "false")
        boolean inActiveGame
) {
    public static WithdrawalPreviewResponse from(WithdrawalPreviewResult result) {
        return new WithdrawalPreviewResponse(result.nickname(), result.inActiveGame());
    }
}
