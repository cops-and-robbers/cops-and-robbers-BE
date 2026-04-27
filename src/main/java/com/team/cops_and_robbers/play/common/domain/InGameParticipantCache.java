package com.team.cops_and_robbers.play.common.domain;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;

public record InGameParticipantCache(
        String nickname,
        Team team,
        String fcmToken
) {
    public static InGameParticipantCache from(GameParticipant participant, String fcmToken) {
        return new InGameParticipantCache(
                participant.getUser().getNickname(),
                participant.getTeam(),
                fcmToken
        );
    }
}
