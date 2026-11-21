package com.team.cops_and_robbers.play.location.application.dto.result;

import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;

import java.util.List;

public record GameStateResult(
        GameMode gameMode,
        List<RunnerLocationInfo> runnerLocations,
        List<ParticipantInfo> participants
) {
    public record RunnerLocationInfo(
            Long participantId,
            String nickname,
            Double latitude,
            Double longitude
    ) {
        public static RunnerLocationInfo from(SystemEventData.RunnerLocation location) {
            return new RunnerLocationInfo(
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
