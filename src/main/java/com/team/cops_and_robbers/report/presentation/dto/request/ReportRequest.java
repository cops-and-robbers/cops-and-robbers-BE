package com.team.cops_and_robbers.report.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportRequest(
        @NotNull
        @Schema(description = "게임 ID", example = "3")
        Long gameId,

        @NotNull
        @Schema(description = "신고 대상 참가자 ID", example = "12")
        Long reportedParticipantId,

        @NotBlank
        @Schema(description = "신고된 메시지 내용", example = "나쁜 말")
        String messageContent
) {
}
