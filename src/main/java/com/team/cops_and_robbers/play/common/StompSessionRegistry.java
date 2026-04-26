package com.team.cops_and_robbers.play.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StompSessionRegistry implements WebSocketHandlerDecoratorFactory {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessions.put(session.getId(), session);
                log.info("[StompSessionRegistry] Session registered: sessionId={}, total={}", session.getId(), sessions.size());
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                sessions.remove(session.getId());
                log.info("[StompSessionRegistry] Session removed: sessionId={}, status={}, total={}", session.getId(), closeStatus, sessions.size());
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }

    public Collection<WebSocketSession> getAllSessions() {
        return List.copyOf(sessions.values());
    }

    public int getSessionCount() {
        return sessions.size();
    }
}
