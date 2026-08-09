package com.team.cops_and_robbers.admin.application.dto.command.report;

import com.team.cops_and_robbers.report.domain.ReportStatus;

public record AdminUpdateReportStatusCommand(
        Long reportId,
        ReportStatus status,
        String adminMemo
) {
}
