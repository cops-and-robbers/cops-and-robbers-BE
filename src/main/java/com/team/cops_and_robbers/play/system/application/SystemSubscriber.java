package com.team.cops_and_robbers.play.system.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.constant.StompSubscribeChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode payload = objectMapper.readTree(message.getBody());

            long gameId = payload.path("gameId").asLong(0L);
            if (gameId <= 0L) {
                log.warn("[SystemSubscriber] invalid gameId. payload={}", payload);
                return;
            }

            String destination = StompSubscribeChannel.SYSTEM.getUrl(gameId);
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("[SystemSubscriber] Message processing failed", e);
        }
    }
}
