package com.team.cops_and_robbers.game.game.domain;

public record GameRules(
        CatchOutcome catchOutcome,
        boolean allowsForceArrest,
        boolean endsWhenAllRobbersArrested,
        boolean endsWhenPoliceLeave,
        boolean validatesTeamComposition
) {
    public static final GameRules COPS_AND_ROBBERS = new GameRules(
            CatchOutcome.CAPTURE,
            true,
            true,
            true,
            true
    );

    public static final GameRules EVENT = new GameRules(
            CatchOutcome.NONE,
            false,
            false,
            false,
            false
    );
}
