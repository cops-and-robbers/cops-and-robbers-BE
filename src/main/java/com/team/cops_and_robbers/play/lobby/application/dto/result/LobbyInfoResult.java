package com.team.cops_and_robbers.play.lobby.application.dto.result;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;

import java.util.List;

public record LobbyInfoResult(
        Long myParticipantId,
        Long hostParticipantId,
        Integer maxParticipants,
        Integer locationRevealIntervalMinutes,
        List<ParticipantInfo> participants
) {
    public static LobbyInfoResult of(Game game, GameParticipant myParticipant, GameParticipant hostParticipant, List<ParticipantInfo> participants) {
        return new LobbyInfoResult(
                myParticipant.getId(),
                hostParticipant.getId(),
                game.getMaxParticipants(),
                game.getLocationRevealIntervalMinutes(),
                participants
        );
    }

    public record ParticipantInfo(
            Long participantId,
            String nickname,
            Team team,
            boolean isReady
    ) {
        public static ParticipantInfo from(GameParticipant participant) {
            return new ParticipantInfo(
                    participant.getId(),
                    participant.getUser().getNickname(),
                    participant.getTeam(),
                    participant.isReady()
            );
        }
    }
}
