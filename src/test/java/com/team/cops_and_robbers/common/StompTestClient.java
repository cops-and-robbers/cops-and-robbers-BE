package com.team.cops_and_robbers.common;

import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class StompTestClient {

    private static final String WS_URL = "ws://localhost:%d%s";
    public static final String SOCKET_PATH = "/connection";
    public static final String LEGACY_SOCKET_PATH = "/game-connection";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int CONNECT_TIMEOUT_SECONDS = 5;

    private final StompSession session;
    private final Map<String, BlockingQueue<?>> receivedMessages = new ConcurrentHashMap<>();

    public StompTestClient(WebSocketStompClient stompClient, int port, String token, String socketPath) throws Exception {
        String url = WS_URL.formatted(port, socketPath);

        StompHeaders headers = new StompHeaders();
        headers.add(AUTH_HEADER, BEARER_PREFIX + token);

        this.session = stompClient
                .connectAsync(url, new WebSocketHttpHeaders(), headers, new EmptySessionHandler())
                .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public <T> void subscribe(String destination, Class<T> type) throws InterruptedException {
        BlockingQueue<T> queue = new LinkedBlockingQueue<>();
        receivedMessages.put(destination, queue);

        session.subscribe(destination, new StompFrameHandlerImpl<>(type, queue));
        Thread.sleep(300);
    }
    
    public <T> T waitForMessage(String destination, int timeoutSeconds) throws InterruptedException {
        BlockingQueue<T> queue = (BlockingQueue<T>) receivedMessages.get(destination);
        return queue.poll(timeoutSeconds, TimeUnit.SECONDS);
    }

    public void send(String destination, Object payload) {
        session.send(destination, payload);
    }

    public void disconnect() {
        session.disconnect();
    }

    private static class EmptySessionHandler extends StompSessionHandlerAdapter {}
}
