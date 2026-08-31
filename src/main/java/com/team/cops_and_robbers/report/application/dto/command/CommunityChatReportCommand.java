package com.team.cops_and_robbers.report.application.dto.command;

import com.team.cops_and_robbers.report.domain.ReportType;
import com.team.cops_and_robbers.report.presentation.dto.request.CommunityChatReportRequest;

public record CommunityChatReportCommand(
        Long chatMessageId,
        Long reporterUserId,
        ReportType reportType,
        String etcReason
) {
    public static CommunityChatReportCommand of(Long reporterUserId, CommunityChatReportRequest request) {
        return new CommunityChatReportCommand(
                request.chatMessageId(),
                reporterUserId,
                request.reportType(),
                request.etcReason()
        );
    }
}
