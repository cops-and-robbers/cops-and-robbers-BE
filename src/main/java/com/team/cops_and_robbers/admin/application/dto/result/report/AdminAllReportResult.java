package com.team.cops_and_robbers.admin.application.dto.result.report;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.report.domain.ReportSource;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import com.team.cops_and_robbers.report.repository.AdminAllReportRow;

public record AdminAllReportResult(
        Long id,
        ReportSource source,
        Long reporterUserId,
        String reporterNickname,
        Long reportedUserId,
        String reportedNickname,
        String content,
        ReportStatus status,
        String createdAt
) {

    private static final String UNKNOWN_USER = "알수없음";

    public static AdminAllReportResult of(AdminAllReportRow row, String reporterNickname, String reportedNickname) {
        return new AdminAllReportResult(
                row.getId(),
                ReportSource.valueOf(row.getSource()),
                row.getReporterUserId(),
                reporterNickname != null ? reporterNickname : UNKNOWN_USER,
                row.getReportedUserId(),
                reportedNickname != null ? reportedNickname : UNKNOWN_USER,
                row.getContent(),
                ReportStatus.valueOf(row.getStatus()),
                TimestampUtil.toIsoString(row.getCreatedAt())
        );
    }
}
