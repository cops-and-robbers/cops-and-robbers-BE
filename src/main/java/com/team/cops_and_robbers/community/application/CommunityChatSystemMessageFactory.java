package com.team.cops_and_robbers.community.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.CommunityChatSystemEventData;
import com.team.cops_and_robbers.community.domain.CommunityChatSystemEventType;
import com.team.cops_and_robbers.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityChatSystemMessageFactory {

    private final ObjectMapper objectMapper;

    public CommunityChatMessage createJoinMessage(Long postId, User user) {
        return createSystemMessage(postId, user, CommunityChatSystemEventType.JOIN);
    }

    public CommunityChatMessage createLeaveMessage(Long postId, User user) {
        return createSystemMessage(postId, user, CommunityChatSystemEventType.LEAVE);
    }

    private CommunityChatMessage createSystemMessage(Long postId, User user, CommunityChatSystemEventType event) {
        return CommunityChatMessage.createMessage(
                UUID.randomUUID().toString(),
                postId,
                user.getId(),
                user.getNickname(),
                serialize(event),
                CommunityChatMessageType.SYSTEM
        );
    }

    private String serialize(CommunityChatSystemEventType event) {
        try {
            return objectMapper.writeValueAsString(CommunityChatSystemEventData.of(event));
        } catch (JsonProcessingException e) {
            log.error("[CommunityChat] System event serialization failed | event={}", event, e);
            return String.format("{\"event\":\"%s\"}", event.name());
        }
    }
}