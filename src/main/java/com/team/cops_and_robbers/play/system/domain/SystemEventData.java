package com.team.cops_and_robbers.play.system.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    record ArrestData(SystemParticipantInfo chaser, SystemParticipantInfo runner,
                      int remainingRunners) implements SystemEventData {
        // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
        @Deprecated
        @JsonProperty(value = "police", access = JsonProperty.Access.READ_ONLY)
        public SystemParticipantInfo police() {
            return chaser;
        }

        @Deprecated
        @JsonProperty(value = "robber", access = JsonProperty.Access.READ_ONLY)
        public SystemParticipantInfo robber() {
            return runner;
        }

        @Deprecated
        @JsonProperty(value = "remainingThieves", access = JsonProperty.Access.READ_ONLY)
        public int remainingThieves() {
            return remainingRunners;
        }

        public static ArrestData of(GameParticipant chaser, GameParticipant runner, int remainingRunners) {
            return new ArrestData(SystemParticipantInfo.from(chaser), SystemParticipantInfo.from(runner), remainingRunners);
        }

        public static ArrestData ofForced(GameParticipant runner, int remainingRunners) {
            return new ArrestData(null, SystemParticipantInfo.from(runner), remainingRunners);
        }
    }

    record EscapeData(SystemParticipantInfo escapedRunner, int remainingRunners) implements SystemEventData {
        // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
        @Deprecated
        @JsonProperty(value = "escapedThief", access = JsonProperty.Access.READ_ONLY)
        public SystemParticipantInfo escapedThief() {
            return escapedRunner;
        }

        @Deprecated
        @JsonProperty(value = "remainingThieves", access = JsonProperty.Access.READ_ONLY)
        public int remainingThieves() {
            return remainingRunners;
        }

        public static EscapeData of(GameParticipant escapedRunner, int remainingRunners) {
            return new EscapeData(SystemParticipantInfo.from(escapedRunner), remainingRunners);
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
