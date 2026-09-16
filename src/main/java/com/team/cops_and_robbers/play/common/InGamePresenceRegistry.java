package com.team.cops_and_robbers.play.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 게임 채널 구독 세션을 참가자 단위로 추적한다.
 */
@Slf4j
@Component
public class InGamePresenceRegistry {

    private final Map<Long, Map<Long, Set<String>>> sessionsByGame = new ConcurrentHashMap<>();
    private final Map<String, Presence> presenceBySession = new ConcurrentHashMap<>();

    public void register(Long gameId, Long participantId, String sessionId) {
        if (sessionId == null) return;
        presenceBySession.put(sessionId, new Presence(gameId, participantId));
        sessionsByGame
                .computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(participantId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
    }

    public boolean isOnline(Long gameId, Long participantId) {
        Map<Long, Set<String>> participants = sessionsByGame.get(gameId);
        if (participants == null) return false;
        Set<String> sessions = participants.get(participantId);
        return sessions != null && !sessions.isEmpty();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        unregister(event.getSessionId());
    }

    void unregister(String sessionId) {
        Presence presence = presenceBySession.remove(sessionId);
        if (presence == null) return;

        Map<Long, Set<String>> participants = sessionsByGame.get(presence.gameId());
        if (participants == null) return;

        participants.computeIfPresent(presence.participantId(), (id, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
        sessionsByGame.computeIfPresent(presence.gameId(), (id, map) -> map.isEmpty() ? null : map);
        log.debug("[Presence] offline: gameId={}, participantId={}", presence.gameId(), presence.participantId());
    }

    private record Presence(Long gameId, Long participantId) {
    }
}
