package com.team.cops_and_robbers.admin.application.dto.result.bug;

import com.team.cops_and_robbers.bug.domain.BugReport;
import com.team.cops_and_robbers.bug.domain.BugReportStatus;
import com.team.cops_and_robbers.common.util.TimestampUtil;

public record AdminBugReportResult(
        Long id,
        String content,
        Long userId,
        String userNickname,
        BugReportStatus status,
        String adminMemo,
        String createdAt
) {

    private static final String UNKNOWN_USER = "알수없음";

    public static AdminBugReportResult of(BugReport bugReport, String userNickname) {
        return new AdminBugReportResult(
                bugReport.getId(),
                bugReport.getContent(),
                bugReport.getUserId(),
                userNickname != null ? userNickname : UNKNOWN_USER,
                bugReport.getStatus(),
                bugReport.getAdminMemo(),
                TimestampUtil.toIsoString(bugReport.getCreatedAt())
        );
    }
}
