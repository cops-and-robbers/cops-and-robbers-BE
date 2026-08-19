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
    LOCATION_ROBBER("/subscribe/game/%s/location/robber"),

    PING_POLICE("/subscribe/game/%s/ping/police"),
    PING_ROBBER("/subscribe/game/%s/ping/robber"),

    COMMUNITY_CHAT("/subscribe/community/%s/chat");

    private final String format;

    /**
     * 게임 채널은 gameId, 커뮤니티 채널은 postId를 받는다.
     */
    public String getUrl(Long id) {
        return String.format(format, id);
    }
}
