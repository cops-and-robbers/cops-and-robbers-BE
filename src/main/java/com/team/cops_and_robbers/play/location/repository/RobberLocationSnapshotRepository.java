package com.team.cops_and_robbers.play.location.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.util.AesEncryptor;
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
        Map<String, EncryptedRobberLocation> entries = locations.stream()
                .collect(Collectors.toMap(
                        loc -> field(loc.participantId()),
                        loc -> new EncryptedRobberLocation(
                                loc.participantId(),
                                loc.nickname(),
                                aesEncryptor.encrypt(String.valueOf(loc.latitude())),
                                aesEncryptor.encrypt(String.valueOf(loc.longitude()))
                        )
                ));
        redisTemplate.opsForHash().putAll(key(gameId), entries);
        redisTemplate.expire(key(gameId), TTL);
    }

    public List<SystemEventData.RobberLocation> findAllByGameId(Long gameId) {
        return redisTemplate.opsForHash().values(key(gameId)).stream()
                .map(value -> objectMapper.convertValue(value, EncryptedRobberLocation.class))
                .map(enc -> SystemEventData.RobberLocation.of(
                        enc.participantId(),
                        enc.nickname(),
                        Double.parseDouble(aesEncryptor.decrypt(enc.latitude())),
                        Double.parseDouble(aesEncryptor.decrypt(enc.longitude()))
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

    private record EncryptedRobberLocation(
            Long participantId,
            String nickname,
            String latitude,
            String longitude
    ) {}
}
