package com.team.cops_and_robbers.play.common.domain;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;

public record InGameParticipantCache(
        String nickname,
        Team team
) {
    public static InGameParticipantCache from(GameParticipant participant) {
        return new InGameParticipantCache(
                participant.getUser().getNickname(),
                participant.getTeam()
        );
    }
}
