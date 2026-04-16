package com.team.cops_and_robbers.play.common.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class InGameParticipantCacheRepository {

    private static final String KEY_PREFIX = "game:";
    private static final String KEY_SUFFIX = ":participants";
    private static final Duration TTL = Duration.ofHours(3).plusMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveAll(Long gameId, List<GameParticipant> participants) {
        Map<String, InGameParticipantCache> entries = participants.stream()
                .collect(Collectors.toMap(
                        p -> field(p.getId()),
                        InGameParticipantCache::from
                ));
        redisTemplate.opsForHash().putAll(key(gameId), entries);
        redisTemplate.expire(key(gameId), TTL);
    }

    public Optional<InGameParticipantCache> findByParticipantId(Long gameId, Long participantId) {
        Object value = redisTemplate.opsForHash().get(key(gameId), field(participantId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.convertValue(value, InGameParticipantCache.class));
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
