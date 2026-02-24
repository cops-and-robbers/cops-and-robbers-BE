package com.team.cops_and_robbers.history.domain;

public record GameHighlights(
        MvpRecord policeMvp,
        MvpRecord robberMvp
) {
    public static GameHighlights of(MvpRecord policeMvp, MvpRecord robberMvp) {
        return new GameHighlights(policeMvp, robberMvp);
    }

    public record MvpRecord(Long userId, String nickname, int score) {
        public static MvpRecord of(Long userId, String nickname, int score) {
            return new MvpRecord(userId, nickname, score);
        }
    }
}
