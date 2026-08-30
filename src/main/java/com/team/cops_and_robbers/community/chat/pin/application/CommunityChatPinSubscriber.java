package com.team.cops_and_robbers.community.chat.pin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.constant.StompSubscribeChannel;
import com.team.cops_and_robbers.community.chat.pin.domain.CommunityChatPinPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityChatPinSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CommunityChatPinPayload payload = objectMapper.readValue(message.getBody(), CommunityChatPinPayload.class);
            String destination = StompSubscribeChannel.COMMUNITY_CHAT_PIN.getUrl(payload.postId());

            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("[CommunityChatPinSubscriber] Message processing failed", e);
        }
    }
}
