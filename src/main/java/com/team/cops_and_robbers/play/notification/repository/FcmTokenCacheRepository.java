package com.team.cops_and_robbers.play.notification.repository;

import com.team.cops_and_robbers.game.participant.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FcmTokenCacheRepository {

    private static final String KEY_PREFIX = "fcm-tokens:";
    private static final String EMPTY = "EMPTY";
    private final RedisTemplate<String, Object> redisTemplate;

    public List<String> findGameTokens(Long gameId) {
        return find(gameKey(gameId));
    }

    public List<String> findTeamTokens(Long gameId, Team team) {
        return find(teamKey(gameId, team));
    }

    public void saveGameTokens(Long gameId, List<String> tokens) {
        save(gameKey(gameId), tokens);
    }

    public void saveTeamTokens(Long gameId, Team team, List<String> tokens) {
        save(teamKey(gameId, team), tokens);
    }

    public void deleteAllByGameId(Long gameId) {
        redisTemplate.delete(List.of(
                gameKey(gameId),
                teamKey(gameId, Team.POLICE),
                teamKey(gameId, Team.ROBBER)
        ));
    }

    @SuppressWarnings("unchecked")
    private List<String> find(String key) {
        Object cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue == null) return null;
        if (EMPTY.equals(cachedValue)) return List.of();

        List<String> cachedTokens = (List<String>) cachedValue;
        return List.copyOf(cachedTokens);
    }

    private void save(String key, List<String> tokens) {
        Object valueToCache = tokens.isEmpty() ? EMPTY : tokens;
        redisTemplate.opsForValue().set(key, valueToCache, Duration.ofMinutes(190));
    }

    private String gameKey(Long gameId) {
        return KEY_PREFIX + gameId;
    }

    private String teamKey(Long gameId, Team team) {
        return KEY_PREFIX + gameId + ":" + team.name();
    }
}
