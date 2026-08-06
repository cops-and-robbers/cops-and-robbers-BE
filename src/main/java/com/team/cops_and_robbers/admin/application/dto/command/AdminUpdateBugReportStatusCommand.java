package com.team.cops_and_robbers.admin.application.dto.command;

import com.team.cops_and_robbers.bug.domain.BugReportStatus;

public record AdminUpdateBugReportStatusCommand(
        Long bugReportId,
        BugReportStatus status,
        String adminMemo
) {
}
