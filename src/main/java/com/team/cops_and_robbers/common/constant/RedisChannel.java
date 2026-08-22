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
    LOCATION_ROBBER("game:%s:location:robber"),

    PING_POLICE("game:%s:ping:police"),
    PING_ROBBER("game:%s:ping:robber"),

    COMMUNITY_CHAT("community:%s:chat");

    private final String format;

    public String getPattern() {
        return String.format(format, "*");
    }

    /**
     * 게임 채널은 gameId, 커뮤니티 채널은 postId를 받는다.
     */
    public String getTopic(Long id) {
        return String.format(format, id);
    }
}
