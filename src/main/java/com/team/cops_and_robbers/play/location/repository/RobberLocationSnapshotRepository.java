package com.team.cops_and_robbers.play.location.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.util.AesEncryptor;
import com.team.cops_and_robbers.play.location.domain.RobberLocationHash;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RobberLocationSnapshotRepository {

    private static final String KEY_PREFIX = "game:";
    private static final String KEY_SUFFIX = ":robber-location-snapshot";
    private static final Duration TTL = Duration.ofMinutes(35);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AesEncryptor aesEncryptor;

    public void saveAll(Long gameId, List<SystemEventData.RobberLocation> locations) {
        Map<String, RobberLocationHash> entries = locations.stream()
                .collect(Collectors.toMap(
                        location -> field(location.participantId()),
                        location -> new RobberLocationHash(
                                location.participantId(),
                                location.nickname(),
                                aesEncryptor.encrypt(String.valueOf(location.latitude())),
                                aesEncryptor.encrypt(String.valueOf(location.longitude()))
                        )
                ));
        redisTemplate.opsForHash().putAll(key(gameId), entries);
        redisTemplate.expire(key(gameId), TTL);
    }

    public List<SystemEventData.RobberLocation> findAllByGameId(Long gameId) {
        return redisTemplate.opsForHash().values(key(gameId)).stream()
                .map(value -> objectMapper.convertValue(value, RobberLocationHash.class))
                .map(location -> SystemEventData.RobberLocation.of(
                        location.participantId(),
                        location.nickname(),
                        Double.parseDouble(aesEncryptor.decrypt(location.encryptedLatitude())),
                        Double.parseDouble(aesEncryptor.decrypt(location.encryptedLongitude()))
                ))
                .toList();
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
