package com.team.cops_and_robbers.play.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GracefulShutdownHandler implements SmartLifecycle {

    private static final int BATCH_SIZE = 10;
    private static final int BATCH_DELAY_MS = 300;

    private final StompSessionRegistry sessionRegistry;
    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop(Runnable callback) {
        try {
            List<WebSocketSession> sessions = new ArrayList<>(sessionRegistry.getAllSessions());

            if (sessions.isEmpty()) {
                log.info("[GracefulShutdown] No active WebSocket sessions. Proceeding with shutdown.");
            } else {
                closeSessionsInBatches(sessions);
            }
        } finally {
            running = false;
            callback.run();
        }
    }

    @Override
    public void stop() {
        stop(() -> {});
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    /**
     * 연결된 WebSocket 세션을 BATCH_SIZE 씩 나누어 순차적으로 종료한다.
     *  - 각 배치 사이에 BATCH_DELAY_MS 만큼 대기하여 재연결 요청을 분산
     *  - 인터럽트 발생 시 남은 세션을 즉시 일괄 종료
     */
    private void closeSessionsInBatches(List<WebSocketSession> sessions) {
        int total = sessions.size();
        log.info("[GracefulShutdown] Starting sequential shutdown of WebSocket sessions: total={}", total);

        try {
            for (int i = 0; i < total; i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, total);
                List<WebSocketSession> batch = sessions.subList(i, Math.min(i + BATCH_SIZE, total));
                batch.forEach(this::closeSession);

                log.info("[GracefulShutdown] Batch shutdown completed: {}/{}", end, total);

                if (end < total) {
                    Thread.sleep(BATCH_DELAY_MS);
                }
            }
            log.info("[GracefulShutdown] All WebSocket sessions successfully closed.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[GracefulShutdown] Interrupted during shutdown. Immediately closing remaining sessions.");
            sessions.stream()
                    .filter(WebSocketSession::isOpen)
                    .forEach(this::closeSession);
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
