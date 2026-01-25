package com.team.cops_and_robbers.play.chat.domain;

import com.team.cops_and_robbers.game.participant.domain.Team;

public record ChatSender(
        Long participantId,
        String nickname,
        Team team
) {
    public static ChatSender of(Long participantId, String nickname, Team team) {
        return new ChatSender(
                participantId,
                nickname,
                team
        );
    }
}
