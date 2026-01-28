package com.team.cops_and_robbers.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedisChannel {

    LOBBY("game:%s:lobby"),
    SYSTEM("game:%s:system"),

    CHAT_ALL("game:%s:chat:all"),
    CHAT_POLICE("game:%s:chat:police"),
    CHAT_ROBBER("game:%s:chat:robber"),

    LOCATION_POLICE("game:%s:location:police"),
    LOCATION_ROBBER("game:%s:location:robber");

    private final String format;

    public String getPattern() {
        return String.format(format, "*");
    }

    public String getTopic(Long gameId) {
        return String.format(format, gameId);
    }
}
