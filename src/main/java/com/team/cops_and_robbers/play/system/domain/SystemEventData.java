package com.team.cops_and_robbers.play.system.domain;

import java.util.List;

public sealed interface SystemEventData permits
        SystemEventData.PoliceMoveStartData,
        SystemEventData.RobberLocationRevealData {

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
}
