package com.team.cops_and_robbers.play.distance.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.play.distance.domain.ParticipantDistanceHash;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ParticipantDistanceRepository {

    private static final String KEY_PREFIX = "game:";
    private static final String KEY_SUFFIX = ":distance";
    private static final Duration TTL = Duration.ofHours(3).plusMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(Long gameId, Long participantId, ParticipantDistanceHash distance) {
        redisTemplate.opsForHash().put(key(gameId), field(participantId), distance);
        redisTemplate.expire(key(gameId), TTL);
    }

    public Optional<ParticipantDistanceHash> findByParticipantId(Long gameId, Long participantId) {
        Object value = redisTemplate.opsForHash().get(key(gameId), field(participantId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.convertValue(value, ParticipantDistanceHash.class));
    }

    public Map<Long, ParticipantDistanceHash> findAllByGameId(Long gameId) {
        return redisTemplate.opsForHash().entries(key(gameId)).entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Long.valueOf(String.valueOf(e.getKey())),
                        e -> objectMapper.convertValue(e.getValue(), ParticipantDistanceHash.class)
                ));
    }

    public void deleteAllByGameId(Long gameId) {
        redisTemplate.delete(key(gameId));
    }

    private String key(Long gameId) {
        return KEY_PREFIX + gameId + KEY_SUFFIX;
    }

    private String field(Long participantId) {
        return String.valueOf(participantId);
    }
}
