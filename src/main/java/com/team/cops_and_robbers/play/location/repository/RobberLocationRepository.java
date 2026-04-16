package com.team.cops_and_robbers.play.location.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.util.AesEncryptor;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RobberLocationRepository {

    private static final String KEY_PREFIX = "game:";
    private static final String KEY_SUFFIX = ":robber-location";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AesEncryptor aesEncryptor;

    public void save(Long gameId, Long participantId, SystemEventData.RobberLocation location) {
        EncryptedRobberLocation encrypted = new EncryptedRobberLocation(
                location.participantId(),
                location.nickname(),
                aesEncryptor.encrypt(String.valueOf(location.latitude())),
                aesEncryptor.encrypt(String.valueOf(location.longitude()))
        );
        redisTemplate.opsForHash().put(key(gameId), field(participantId), encrypted);
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

    /**
     * Redis 저장용 내부 DTO
     *   - lat/lon을 암호화된 문자열로 보관
     */
    private record EncryptedRobberLocation(
            Long participantId,
            String nickname,
            String latitude,
            String longitude
    ) {}
}
