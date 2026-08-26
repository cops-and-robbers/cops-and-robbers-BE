package com.team.cops_and_robbers.admin.application.dto.result.report;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.report.domain.CommunityChatReport;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import com.team.cops_and_robbers.report.domain.ReportType;

public record AdminCommunityChatReportResult(
        Long id,
        Long chatMessageId,
        Long reporterUserId,
        String reporterNickname,
        Long reportedUserId,
        String reportedNickname,
        String messageContent,
        ReportType reportType,
        String etcReason,
        ReportStatus status,
        String adminMemo,
        String createdAt
) {

    private static final String WITHDRAWN_USER = "탈퇴한 사용자";

    public static AdminCommunityChatReportResult of(CommunityChatReport report, String reporterNickname, String reportedNickname) {
        return new AdminCommunityChatReportResult(
                report.getId(),
                report.getChatMessageId(),
                report.getReporterUserId(),
                reporterNickname != null ? reporterNickname : WITHDRAWN_USER,
                report.getReportedUserId(),
                reportedNickname != null ? reportedNickname : WITHDRAWN_USER,
                report.getMessageContent(),
                report.getReportType(),
                report.getEtcReason(),
                report.getStatus(),
                report.getAdminMemo(),
                TimestampUtil.toIsoString(report.getCreatedAt())
        );
    }
}
