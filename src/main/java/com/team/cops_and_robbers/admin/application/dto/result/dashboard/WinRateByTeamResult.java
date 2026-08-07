package com.team.cops_and_robbers.admin.application.dto.result.dashboard;

public record WinRateByTeamResult(
        Double policeWinRate,
        Double robberWinRate
) {
}
