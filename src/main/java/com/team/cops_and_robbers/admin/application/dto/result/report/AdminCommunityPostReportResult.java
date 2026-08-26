package com.team.cops_and_robbers.admin.application.dto.result.report;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.report.domain.CommunityPostReport;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import com.team.cops_and_robbers.report.domain.ReportType;

public record AdminCommunityPostReportResult(
        Long id,
        Long postId,
        String postTitle,
        String postContent,
        Long reporterUserId,
        String reporterNickname,
        Long reportedUserId,
        String reportedNickname,
        ReportType reportType,
        String etcReason,
        ReportStatus status,
        String adminMemo,
        String createdAt
) {

    private static final String WITHDRAWN_USER = "탈퇴한 사용자";

    public static AdminCommunityPostReportResult of(CommunityPostReport report, String reporterNickname, String reportedNickname) {
        return new AdminCommunityPostReportResult(
                report.getId(),
                report.getPostId(),
                report.getPostTitle(),
                report.getPostContent(),
                report.getReporterUserId(),
                reporterNickname != null ? reporterNickname : WITHDRAWN_USER,
                report.getReportedUserId(),
                reportedNickname != null ? reportedNickname : WITHDRAWN_USER,
                report.getReportType(),
                report.getEtcReason(),
                report.getStatus(),
                report.getAdminMemo(),
                TimestampUtil.toIsoString(report.getCreatedAt())
        );
    }
}
