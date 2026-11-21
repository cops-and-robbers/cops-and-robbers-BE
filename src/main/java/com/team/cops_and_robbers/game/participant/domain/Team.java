package com.team.cops_and_robbers.game.participant.domain;

import java.util.concurrent.ThreadLocalRandom;

public enum Team {
    CHASER,
    RUNNER;

    private static final Team[] VALUES = values();

    public static Team getRandomTeam() {
        int randomIndex = ThreadLocalRandom.current().nextInt(VALUES.length);
        return VALUES[randomIndex];
    }
}
