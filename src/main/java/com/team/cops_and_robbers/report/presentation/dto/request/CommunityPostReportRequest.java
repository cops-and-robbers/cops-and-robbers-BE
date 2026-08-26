package com.team.cops_and_robbers.report.presentation.dto.request;

import com.team.cops_and_robbers.report.domain.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommunityPostReportRequest(
        @NotNull
        @Schema(description = "신고할 모집글 ID", example = "3")
        Long postId,

        @NotNull
        @Schema(description = "신고 유형", example = "SPAM")
        ReportType reportType,

        @Size(max = 300)
        @Schema(description = "기타 사유 (신고 유형이 ETC일 때 필수)", example = "기타 사유")
        String etcReason
) {
}
