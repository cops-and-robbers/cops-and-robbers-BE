package com.team.cops_and_robbers.game.game.domain;

public enum GameMode {
    COPS_AND_ROBBERS(true),
    TAG(false);

    private final boolean supportsEventGame;

    GameMode(boolean supportsEventGame) {
        this.supportsEventGame = supportsEventGame;
    }

    public boolean supportsEventGame() {
        return supportsEventGame;
    }
}
