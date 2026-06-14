package com.team.cops_and_robbers.game.participant.domain;

import java.util.concurrent.ThreadLocalRandom;

public enum Team {
    POLICE("경찰"),
    ROBBER("도둑");

    private static final Team[] VALUES = values();
    private final String displayName;

    Team(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Team getRandomTeam() {
        int randomIndex = ThreadLocalRandom.current().nextInt(VALUES.length);
        return VALUES[randomIndex];
    }
}
