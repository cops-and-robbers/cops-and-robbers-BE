package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.game.game.application.dto.result.GameCreateResult;
import java.time.LocalDateTime;

public record GameCreateResponse(
        Long gameId,
        String inviteCode,
        String status,
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants,
        LocalDateTime createdAt
) {

    public static GameCreateResponse from(GameCreateResult result) {
        return new GameCreateResponse(
                result.gameId(),
                result.inviteCode(),
                result.status(),
                result.roundDurationMinutes(),
                result.locationRevealIntervalMinutes(),
                result.policeWaitMinutes(),
                result.maxParticipants(),
                result.createdAt()
        );
    }
}
