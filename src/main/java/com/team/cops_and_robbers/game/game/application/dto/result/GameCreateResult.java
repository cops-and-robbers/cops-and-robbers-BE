package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.game.game.domain.Game;
import java.time.LocalDateTime;

public record GameCreateResult(
        Long gameId,
        String inviteCode,
        String status,
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants,
        LocalDateTime createdAt
) {

    public static GameCreateResult from(Game game) {
        return new GameCreateResult(
                game.getId(),
                game.getInviteCode(),
                game.getStatus().name(),
                game.getRoundDurationMinutes(),
                game.getLocationRevealIntervalMinutes(),
                game.getPoliceWaitMinutes(),
                game.getMaxParticipants(),
                game.getCreatedAt()
        );
    }
}
