package com.team.cops_and_robbers.play.system.domain;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;

import java.util.List;

public sealed interface SystemEventData permits
        SystemEventData.ChaserMoveStartData,
        SystemEventData.RunnerLocationRevealData,
        SystemEventData.ArrestData,
        SystemEventData.EscapeData,
        SystemEventData.GameEndData,
        SystemEventData.PlayerLeftData {

    record SystemParticipantInfo(Long participantId, String nickname, ParticipantStatus status) {
        public static SystemParticipantInfo from(GameParticipant participant) {
            return new SystemParticipantInfo(
                    participant.getId(),
                    participant.getUser().getNickname(),
                    participant.getStatus()
            );
        }
    }

    record ChaserMoveStartData() implements SystemEventData {
        public static ChaserMoveStartData create() {
            return new ChaserMoveStartData();
        }
    }

    record RunnerLocation(Long participantId, String nickname, Double latitude, Double longitude) {
        public static RunnerLocation of(Long participantId, String nickname, Double latitude, Double longitude) {
            return new RunnerLocation(participantId, nickname, latitude, longitude);
        }
    }

    record RunnerLocationRevealData(List<RunnerLocation> locations) implements SystemEventData {
        public static RunnerLocationRevealData of(List<RunnerLocation> locations) {
            return new RunnerLocationRevealData(locations);
        }
    }

    record ArrestData(SystemParticipantInfo police, SystemParticipantInfo robber,
                      int remainingThieves) implements SystemEventData {
        public static ArrestData of(GameParticipant police, GameParticipant robber, int remainingThieves) {
            return new ArrestData(SystemParticipantInfo.from(police), SystemParticipantInfo.from(robber), remainingThieves);
        }

        public static ArrestData ofForced(GameParticipant robber, int remainingThieves) {
            return new ArrestData(null, SystemParticipantInfo.from(robber), remainingThieves);
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

    record PlayerLeftData(Long participantId, String nickname, Team team) implements SystemEventData {
        public static PlayerLeftData of(GameParticipant participant) {
            return new PlayerLeftData(
                    participant.getId(),
                    participant.getUser().getNickname(),
                    participant.getTeam()
            );
        }
    }
}
