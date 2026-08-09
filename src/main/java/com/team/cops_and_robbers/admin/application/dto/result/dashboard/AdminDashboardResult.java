package com.team.cops_and_robbers.admin.application.dto.result.dashboard;

import java.util.List;

public record AdminDashboardResult(
        Long todayGameCount,
        Long weeklyGameCount,
        Long inProgressGameCount,
        Long totalUserCount,
        Long todayNewUserCount,
        Long pendingReportCount,
        Long pendingBugReportCount,
        List<EndReasonDistributionResult> endReasonDistribution,
        Double averageGameDurationSeconds,
        WinRateByTeamResult winRateByTeam
) {
}
