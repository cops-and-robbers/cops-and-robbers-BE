package com.team.cops_and_robbers.play.chat.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.constant.StompSubscribeChannel;
import com.team.cops_and_robbers.play.chat.domain.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllChatSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message.getBody(), ChatMessage.class);
            String destination = StompSubscribeChannel.CHAT_ALL.getUrl(chatMessage.gameId());

            messagingTemplate.convertAndSend(destination, chatMessage);
        } catch (Exception e) {
            log.error("[AllChatSubscriber] Message processing failed", e);
        }
    }
}
