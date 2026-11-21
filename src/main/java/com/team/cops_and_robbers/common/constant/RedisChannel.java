package com.team.cops_and_robbers.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedisChannel {

    LOBBY("game:%s:lobby"),
    SYSTEM("game:%s:system"),

    CHAT_ALL("game:%s:chat:all"),
    CHAT_CHASER("game:%s:chat:chaser"),
    CHAT_RUNNER("game:%s:chat:runner"),

    LOCATION_CHASER("game:%s:location:chaser"),
    LOCATION_RUNNER("game:%s:location:runner"),

    PING_CHASER("game:%s:ping:chaser"),
    PING_RUNNER("game:%s:ping:runner"),

    COMMUNITY_CHAT("community:%s:chat"),
    COMMUNITY_CHAT_PIN("community:%s:chat:pin");

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
