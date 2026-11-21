package com.team.cops_and_robbers.history.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum GameEndReason {
    ALL_ARRESTED,
    TIME_OVER,
    CHASER_FORFEITED("POLICE_FORFEITED"),
    RUNNER_FORFEITED("ROBBER_FORFEITED");

    private final String legacyName;

    GameEndReason() {
        this.legacyName = name();
    }

    GameEndReason(String legacyName) {
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
    public static GameEndReason from(String value) {
        return Arrays.stream(values())
                .filter(reason -> reason.name().equals(value) || reason.legacyName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown game end reason: " + value));
    }
}
