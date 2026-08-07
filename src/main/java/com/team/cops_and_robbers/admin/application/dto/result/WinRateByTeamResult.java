package com.team.cops_and_robbers.admin.application.dto.result;

public record WinRateByTeamResult(
        Double policeWinRate,
        Double robberWinRate
) {
}
