package com.team.cops_and_robbers.play.common.domain;

import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantCacheProjection;

public record InGameParticipantCache(
        String nickname,
        Team team,
        String fcmToken
) {
    public static InGameParticipantCache from(GameParticipantCacheProjection projection) {
        return new InGameParticipantCache(
                projection.nickname(),
                projection.team(),
                projection.fcmToken()
        );
    }
}
