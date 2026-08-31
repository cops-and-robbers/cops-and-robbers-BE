package com.team.cops_and_robbers.admin.application.dto.result.report;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.report.domain.ReportSource;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import com.team.cops_and_robbers.report.repository.AdminAllReportRow;
import com.team.cops_and_robbers.user.domain.User;

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

    public static AdminAllReportResult of(AdminAllReportRow row, String reporterNickname, String reportedNickname) {
        return new AdminAllReportResult(
                row.getId(),
                ReportSource.valueOf(row.getSource()),
                row.getReporterUserId(),
                reporterNickname != null ? reporterNickname : User.UNKNOWN_NICKNAME,
                row.getReportedUserId(),
                reportedNickname != null ? reportedNickname : User.UNKNOWN_NICKNAME,
                row.getContent(),
                ReportStatus.valueOf(row.getStatus()),
                TimestampUtil.toIsoString(row.getCreatedAt())
        );
    }
}
