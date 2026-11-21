package com.team.cops_and_robbers.play.system.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum SystemEventType {
    CHASER_MOVE_START("POLICE_MOVE_START"),
    RUNNER_LOCATION_REVEAL("ROBBER_LOCATION_REVEAL"),
    ARREST,
    ESCAPE,
    GAME_OVER,
    PLAYER_LEFT;

    private final String legacyName;

    SystemEventType() {
        this.legacyName = name();
    }

    SystemEventType(String legacyName) {
        this.legacyName = legacyName;
    }

    // 옛 앱 호환: 소켓과 FCM은 옛 이름으로 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonValue
    public String legacyName() {
        return legacyName;
    }

    // 옛 앱 호환: Redis에 옛 이름으로 저장된 이벤트도 읽는다. 강제 업데이트 후 제거
    @Deprecated
    @JsonCreator
    public static SystemEventType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(value) || type.legacyName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown system event type: " + value));
    }
}
