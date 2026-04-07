package com.team.cops_and_robbers.play.location.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RobberLocationRepository {

    private static final String KEY_PREFIX = "robber-location:";
    private static final String FIELD_PREFIX = "participant:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(Long gameId, Long participantId, SystemEventData.RobberLocation location) {
        redisTemplate.opsForHash().put(key(gameId), field(participantId), location);
        redisTemplate.expire(key(gameId), Duration.ofMinutes(10));
    }

    public List<SystemEventData.RobberLocation> findAllByGameId(Long gameId) {
        return redisTemplate.opsForHash().values(key(gameId)).stream()
                .map(value -> objectMapper.convertValue(value, SystemEventData.RobberLocation.class))
                .toList();
    }

    public void deleteAllByGameId(Long gameId) {
        redisTemplate.delete(key(gameId));
    }

    private String key(Long gameId) {
        return KEY_PREFIX + gameId;
    }

    private String field(Long participantId) {
        return FIELD_PREFIX + participantId;
    }
}
