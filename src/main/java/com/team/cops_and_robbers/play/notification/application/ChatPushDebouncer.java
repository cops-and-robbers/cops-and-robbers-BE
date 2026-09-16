package com.team.cops_and_robbers.play.notification.application;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 한 게임의 채팅 푸시를 방 단위로 일정 시간에 1건만 내보낸다.
 */
@Component
@RequiredArgsConstructor
public class ChatPushDebouncer {

    private static final String KEY_PREFIX = "fcm:chat:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${fcm.chat.debounce-seconds:30}")
    private long debounceSeconds;

    /**
     * @return 창을 새로 열었으면 true(지금 발송), 이미 열려 있으면 false(스킵).
     */
    public boolean tryOpenWindow(Long gameId) {
        Boolean opened = redisTemplate.opsForValue()
                .setIfAbsent(key(gameId), "1", Duration.ofSeconds(debounceSeconds));
        return Boolean.TRUE.equals(opened);
    }

    private String key(Long gameId) {
        return KEY_PREFIX + gameId;
    }
}
