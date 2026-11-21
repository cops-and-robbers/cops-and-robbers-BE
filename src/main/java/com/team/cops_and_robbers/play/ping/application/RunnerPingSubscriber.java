package com.team.cops_and_robbers.play.ping.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.constant.StompSubscribeChannel;
import com.team.cops_and_robbers.play.ping.domain.PingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunnerPingSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            PingMessage pingMessage = objectMapper.readValue(message.getBody(), PingMessage.class);
            String destination = StompSubscribeChannel.PING_RUNNER.getUrl(pingMessage.gameId());
            messagingTemplate.convertAndSend(destination, pingMessage);
            // 옛 앱 호환. 강제 업데이트 후 제거
            messagingTemplate.convertAndSend(StompSubscribeChannel.PING_ROBBER.getUrl(pingMessage.gameId()), pingMessage);
        } catch (Exception e) {
            log.error("[RunnerPingSubscriber] Message processing failed", e);
        }
    }
}
