package com.team.cops_and_robbers.game.game.domain;

public record GameRules(
        CatchOutcome catchOutcome,
        boolean usesJailArea,
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
            true,
            true
    );

    public static final GameRules TAG = new GameRules(
            CatchOutcome.CAPTURE,
            false,
            true,
            true,
            true,
            true
    );

    public static final GameRules EVENT = new GameRules(
            CatchOutcome.NONE,
            true,
            false,
            false,
            false,
            false
    );
}
