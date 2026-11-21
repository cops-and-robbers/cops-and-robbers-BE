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
    CHAT_CHASER("/subscribe/game/%s/chat/chaser"),
    CHAT_RUNNER("/subscribe/game/%s/chat/runner"),

    LOCATION_CHASER("/subscribe/game/%s/location/chaser"),
    LOCATION_RUNNER("/subscribe/game/%s/location/runner"),

    PING_CHASER("/subscribe/game/%s/ping/chaser"),
    PING_RUNNER("/subscribe/game/%s/ping/runner"),

    COMMUNITY_CHAT("/subscribe/community/%s/chat"),
    COMMUNITY_CHAT_USER("/subscribe/user/%s/community/chat"),
    COMMUNITY_CHAT_PIN("/subscribe/community/%s/chat/pin");

    private final String format;

    /**
     * 게임 채널은 gameId, 커뮤니티 채널은 postId를 받는다.
     * COMMUNITY_CHAT_USER는 채팅방 목록 화면에서 구독 하나로 내 모든 방을 받는 채널이라 userId를 받는다.
     */
    public String getUrl(Long id) {
        return String.format(format, id);
    }
}
