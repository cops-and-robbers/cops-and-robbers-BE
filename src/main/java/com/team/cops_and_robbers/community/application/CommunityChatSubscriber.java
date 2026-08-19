package com.team.cops_and_robbers.community.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.constant.StompSubscribeChannel;
import com.team.cops_and_robbers.community.domain.CommunityChatPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityChatSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CommunityChatPayload payload = objectMapper.readValue(message.getBody(), CommunityChatPayload.class);
            String destination = StompSubscribeChannel.COMMUNITY_CHAT.getUrl(payload.communityPostId());

            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("[CommunityChatSubscriber] Message processing failed", e);
        }
    }
}
