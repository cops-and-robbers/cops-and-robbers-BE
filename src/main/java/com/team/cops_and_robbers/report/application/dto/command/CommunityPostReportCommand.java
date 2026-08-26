package com.team.cops_and_robbers.report.application.dto.command;

import com.team.cops_and_robbers.report.domain.ReportType;
import com.team.cops_and_robbers.report.presentation.dto.request.CommunityPostReportRequest;

public record CommunityPostReportCommand(
        Long postId,
        Long reporterUserId,
        ReportType reportType,
        String etcReason
) {
    public static CommunityPostReportCommand of(Long reporterUserId, CommunityPostReportRequest request) {
        return new CommunityPostReportCommand(
                request.postId(),
                reporterUserId,
                request.reportType(),
                request.etcReason()
        );
    }
}
