package com.team.cops_and_robbers.game.participant.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ParticipantStatus {
    WAITING,
    ALIVE,
    JAILED,
    CHASER_WAITING("POLICE_WAITING");

    private final String legacyName;

    ParticipantStatus() {
        this.legacyName = name();
    }

    ParticipantStatus(String legacyName) {
        this.legacyName = legacyName;
    }

    // 옛 앱 호환: 응답은 옛 이름으로 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonValue
    public String legacyName() {
        return legacyName;
    }

    // 옛 앱 호환: 요청은 옛 이름과 새 이름을 모두 받는다. 강제 업데이트 후 제거
    @Deprecated
    @JsonCreator
    public static ParticipantStatus from(String value) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(value) || status.legacyName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown participant status: " + value));
    }
}
