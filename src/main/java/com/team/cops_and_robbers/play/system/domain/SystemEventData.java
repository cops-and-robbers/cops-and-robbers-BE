package com.team.cops_and_robbers.play.system.domain;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;

import java.util.List;

public sealed interface SystemEventData permits
        SystemEventData.PoliceMoveStartData,
        SystemEventData.RobberLocationRevealData,
        SystemEventData.ArrestData,
        SystemEventData.EscapeData,
        SystemEventData.GameEndData {

    record SystemParticipantInfo(Long participantId, String nickname, ParticipantStatus status) {
        public static SystemParticipantInfo from(GameParticipant participant) {
            return new SystemParticipantInfo(
                    participant.getId(),
                    participant.getUser().getNickname(),
                    participant.getStatus()
            );
        }
    }

    record PoliceMoveStartData() implements SystemEventData {
        public static PoliceMoveStartData create() {
            return new PoliceMoveStartData();
        }
    }

    record RobberLocation(Long participantId, String nickname, Double latitude, Double longitude) {
        public static RobberLocation of(Long participantId, String nickname, Double latitude, Double longitude) {
            return new RobberLocation(participantId, nickname, latitude, longitude);
        }
    }

    record RobberLocationRevealData(List<RobberLocation> locations) implements SystemEventData {
        public static RobberLocationRevealData of(List<RobberLocation> locations) {
            return new RobberLocationRevealData(locations);
        }
    }

    record ArrestData(SystemParticipantInfo police, SystemParticipantInfo robber,
                      int remainingThieves) implements SystemEventData {
        public static ArrestData of(GameParticipant police, GameParticipant robber, int remainingThieves) {
            return new ArrestData(SystemParticipantInfo.from(police), SystemParticipantInfo.from(robber), remainingThieves);
        }
    }

    record EscapeData(SystemParticipantInfo escapedThief, int remainingThieves) implements SystemEventData {
        public static EscapeData of(GameParticipant escapedThief, int remainingThieves) {
            return new EscapeData(SystemParticipantInfo.from(escapedThief), remainingThieves);
        }
    }

    record GameEndData(Long gameResultId, Team winnerTeam, GameEndReason reason) implements SystemEventData {
        public static GameEndData of(Long gameResultId, Team winnerTeam, GameEndReason reason) {
            return new GameEndData(gameResultId, winnerTeam, reason);
        }
    }
}
