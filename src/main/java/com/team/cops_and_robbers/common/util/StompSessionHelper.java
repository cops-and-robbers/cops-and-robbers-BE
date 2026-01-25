package com.team.cops_and_robbers.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StompSessionHelper {

    private static final String USER_SESSION_KEY = "userId";
    private static final String GAME_SESSION_KEY = "gameId";
    private static final String PARTICIPANT_SESSION_KEY = "participantId";

    public static void putUserId(SimpMessageHeaderAccessor accessor, Long userId) {
        getSessionAttributes(accessor).ifPresent(attributes ->
                attributes.put(USER_SESSION_KEY, userId)
        );
    }

    public static Optional<Long> getUserId(SimpMessageHeaderAccessor accessor) {
        return getAttribute(accessor, USER_SESSION_KEY);
    }

    public static void putParticipantId(SimpMessageHeaderAccessor accessor, Long participantId) {
        getSessionAttributes(accessor).ifPresent(attributes ->
                attributes.put(PARTICIPANT_SESSION_KEY, participantId)
        );
    }

    public static Optional<Long> getParticipantId(SimpMessageHeaderAccessor accessor) {
        return getAttribute(accessor, PARTICIPANT_SESSION_KEY);
    }

    public static void putGameId(SimpMessageHeaderAccessor accessor, Long gameId) {
        getSessionAttributes(accessor).ifPresent(attributes ->
                attributes.put(GAME_SESSION_KEY, gameId)
        );
    }

    public static Optional<Long> getGameId(SimpMessageHeaderAccessor accessor) {
        return getAttribute(accessor, GAME_SESSION_KEY);
    }

    private static Optional<Long> getAttribute(SimpMessageHeaderAccessor accessor, String key) {
        return getSessionAttributes(accessor)
                .map(attributes -> attributes.get(key))
                .filter(value -> value instanceof Long)
                .map(value -> (Long) value);
    }

    private static Optional<Map<String, Object>> getSessionAttributes(SimpMessageHeaderAccessor accessor) {
        if (accessor == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(accessor.getSessionAttributes());
    }
}
