package com.team.cops_and_robbers.play.radio.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RadioOverloadRepository {

    private static final String KEY = "radio:overload";

    private final StringRedisTemplate redisTemplate;

    public void activate(Duration ttl) {
        redisTemplate.opsForValue().set(KEY, "1", ttl);
    }

    public void deactivate() {
        redisTemplate.delete(KEY);
    }

    public boolean isActive() {
        return redisTemplate.hasKey(KEY);
    }
}
