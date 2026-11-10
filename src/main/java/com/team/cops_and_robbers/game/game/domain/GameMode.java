package com.team.cops_and_robbers.game.game.domain;

public enum GameMode {
    COPS_AND_ROBBERS(GameRules.COPS_AND_ROBBERS, true),
    TAG(GameRules.TAG, false);

    private final GameRules rules;
    private final boolean supportsEventGame;

    GameMode(GameRules rules, boolean supportsEventGame) {
        this.rules = rules;
        this.supportsEventGame = supportsEventGame;
    }

    public GameRules rules() {
        return rules;
    }

    public boolean supportsEventGame() {
        return supportsEventGame;
    }
}
