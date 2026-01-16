package com.team.cops_and_robbers.game.participant.application.dto.result;

public record GameLeaveResult(
        Long leftUserId,
        Integer remainingCount
) {

    public static GameLeaveResult from(Long leftUserId, int remainingCount) {
        return new GameLeaveResult(
                leftUserId,
                remainingCount
        );
    }
}
