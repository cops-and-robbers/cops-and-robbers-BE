package com.team.cops_and_robbers.game.participant.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public enum Team {
    CHASER("POLICE"),
    RUNNER("ROBBER");

    private static final Team[] VALUES = values();

    private final String legacyName;

    Team(String legacyName) {
        this.legacyName = legacyName;
    }

    public static Team getRandomTeam() {
        int randomIndex = ThreadLocalRandom.current().nextInt(VALUES.length);
        return VALUES[randomIndex];
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
    public static Team from(String value) {
        return Arrays.stream(VALUES)
                .filter(team -> team.name().equals(value) || team.legacyName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown team: " + value));
    }
}
