package com.team.cops_and_robbers.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StompSubscribeChannel {

    LOBBY("/subscribe/game/%s/lobby"),
    SYSTEM("/subscribe/game/%s/system"),
    SYSTEM_FOOTPRINT("/subscribe/game/%s/system/footprint"),

    CHAT_ALL("/subscribe/game/%s/chat/all"),
    CHAT_POLICE("/subscribe/game/%s/chat/police"),
    CHAT_ROBBER("/subscribe/game/%s/chat/robber"),

    LOCATION_POLICE("/subscribe/game/%s/location/police"),
    LOCATION_ROBBER("/subscribe/game/%s/location/robber");

    private final String format;

    public String getUrl(Long gameId) {
        return String.format(format, gameId);
    }
}
