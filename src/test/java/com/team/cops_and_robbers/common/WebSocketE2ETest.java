package com.team.cops_and_robbers.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.ArrayList;
import java.util.List;

public abstract class WebSocketE2ETest extends ControllerTest {

    protected WebSocketStompClient stompClient;
    private final List<StompTestClient> clients = new ArrayList<>();

    @BeforeEach
    protected void setupStomp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    protected void disconnectClients() {
        for (StompTestClient client : clients) {
            client.disconnect();
        }
        clients.clear();
    }

    protected StompTestClient connect(String token) throws Exception {
        return connect(token, StompTestClient.SOCKET_PATH);
    }

    protected StompTestClient connect(String token, String socketPath) throws Exception {
        StompTestClient client = new StompTestClient(stompClient, port, token, socketPath);
        clients.add(client);
        return client;
    }
}
