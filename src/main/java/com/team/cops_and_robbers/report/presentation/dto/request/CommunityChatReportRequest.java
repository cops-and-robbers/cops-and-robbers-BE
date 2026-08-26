package com.team.cops_and_robbers.report.presentation.dto.request;

import com.team.cops_and_robbers.report.domain.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommunityChatReportRequest(
        @NotNull
        @Schema(description = "신고할 채팅 메시지 ID", example = "42")
        Long chatMessageId,

        @NotNull
        @Schema(description = "신고 유형", example = "VERBAL_ABUSE")
        ReportType reportType,

        @Size(max = 300)
        @Schema(description = "기타 사유 (신고 유형이 ETC일 때 필수)", example = "기타 사유")
        String etcReason
) {
}
