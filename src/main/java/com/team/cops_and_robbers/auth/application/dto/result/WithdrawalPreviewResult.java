package com.team.cops_and_robbers.auth.application.dto.result;

import com.team.cops_and_robbers.user.domain.User;

public record WithdrawalPreviewResult(
        String nickname,
        boolean inActiveGame
) {
    public static WithdrawalPreviewResult of(User user, boolean inActiveGame) {
        return new WithdrawalPreviewResult(user.getNickname(), inActiveGame);
    }
}
