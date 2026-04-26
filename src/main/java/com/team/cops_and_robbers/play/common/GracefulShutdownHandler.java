package com.team.cops_and_robbers.play.common;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GracefulShutdownHandler {

    private static final int BATCH_SIZE = 30;
    private static final int BATCH_DELAY_MS = 300;

    private final StompSessionRegistry sessionRegistry;

    @PreDestroy
    public void onShutdown() {
        List<WebSocketSession> allSessions = new ArrayList<>(sessionRegistry.getAllSessions());
        int total = allSessions.size();

        if (total == 0) {
            log.info("[GracefulShutdown] No active WebSocket sessions found. Proceeding with shutdown.");
            return;
        }

        log.info("[GracefulShutdown] Starting sequential shutdown of WebSocket sessions: total={}", total);

        try {
            for (int i = 0; i < total; i += BATCH_SIZE) {
                List<WebSocketSession> batch = allSessions.subList(i, Math.min(i + BATCH_SIZE, total));
                batch.forEach(this::closeSession);

                log.info("[GracefulShutdown] Batch shutdown completed: {}/{}", Math.min(i + BATCH_SIZE, total), total);

                if (i + BATCH_SIZE < total) {
                    Thread.sleep(BATCH_DELAY_MS);
                }
            }
            log.info("[GracefulShutdown] All WebSocket sessions successfully closed.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[GracefulShutdown] Interrupted during shutdown. Immediately closing all remaining sessions.");
            allSessions.forEach(this::closeSession);
        }
    }

    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(new CloseStatus(1001, "Server is restarting"));
            }
        } catch (Exception e) {
            log.warn("[GracefulShutdown] Failed to close session: sessionId={}, error={}", session.getId(), e.getMessage());
        }
    }
}
