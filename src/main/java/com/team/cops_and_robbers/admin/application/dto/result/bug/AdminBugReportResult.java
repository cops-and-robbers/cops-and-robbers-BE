package com.team.cops_and_robbers.admin.application.dto.result.bug;

import com.team.cops_and_robbers.bug.domain.BugReport;
import com.team.cops_and_robbers.bug.domain.BugReportStatus;
import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.user.domain.User;

public record AdminBugReportResult(
        Long id,
        String content,
        Long userId,
        String userNickname,
        BugReportStatus status,
        String adminMemo,
        String createdAt
) {

    public static AdminBugReportResult of(BugReport bugReport, String userNickname) {
        return new AdminBugReportResult(
                bugReport.getId(),
                bugReport.getContent(),
                bugReport.getUserId(),
                userNickname != null ? userNickname : User.UNKNOWN_NICKNAME,
                bugReport.getStatus(),
                bugReport.getAdminMemo(),
                TimestampUtil.toIsoString(bugReport.getCreatedAt())
        );
    }
}
