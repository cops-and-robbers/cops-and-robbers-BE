package com.team.cops_and_robbers.play.location.application.dto.result;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;

import java.util.List;

public record GameStateResult(
        List<RobberLocationInfo> robberLocations,
        List<ParticipantInfo> participants
) {
    public record RobberLocationInfo(
            Long participantId,
            String nickname,
            Double latitude,
            Double longitude
    ) {
        public static RobberLocationInfo from(SystemEventData.RobberLocation location) {
            return new RobberLocationInfo(
                    location.participantId(),
                    location.nickname(),
                    location.latitude(),
                    location.longitude()
            );
        }
    }

    public record ParticipantInfo(
            Long participantId,
            String nickname,
            Team team,
            ParticipantStatus status
    ) {
        public static ParticipantInfo from(GameParticipant participant) {
            return new ParticipantInfo(
                    participant.getId(),
                    participant.getUser().getNickname(),
                    participant.getTeam(),
                    participant.getStatus()
            );
        }
    }
}
