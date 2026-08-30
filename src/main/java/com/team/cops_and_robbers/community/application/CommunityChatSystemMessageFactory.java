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
        return createSystemMessage(
                postId, user.getId(), user.getNickname(), user.getProfileIcon(), CommunityChatSystemEventType.JOIN);
    }

    public CommunityChatMessage createLeaveMessage(Long postId, User user) {
        return createSystemMessage(
                postId, user.getId(), user.getNickname(), user.getProfileIcon(), CommunityChatSystemEventType.LEAVE);
    }

    public CommunityChatMessage createPinRegisteredMessage(Long postId, User hostUser) {
        return createSystemMessage(postId, hostUser.getId(), hostUser.getNickname(), hostUser.getProfileIcon(),
                CommunityChatSystemEventType.PIN_REGISTERED);
    }

    public CommunityChatMessage createPinUpdatedMessage(Long postId, User hostUser) {
        return createSystemMessage(postId, hostUser.getId(), hostUser.getNickname(), hostUser.getProfileIcon(),
                CommunityChatSystemEventType.PIN_UPDATED);
    }

    public CommunityChatMessage createPinDeletedMessage(Long postId, User hostUser) {
        return createSystemMessage(postId, hostUser.getId(), hostUser.getNickname(), hostUser.getProfileIcon(),
                CommunityChatSystemEventType.PIN_DELETED);
    }

    /**
     * 강퇴 대상은 이미 탈퇴해 User 행이 없을 수 있어 User 대신 userId·nickname·아이콘을 직접 받기로 함
     */
    public CommunityChatMessage createKickMessage(
            Long postId, Long targetUserId, String targetNickname, int targetProfileIcon) {
        return createSystemMessage(
                postId, targetUserId, targetNickname, targetProfileIcon, CommunityChatSystemEventType.KICK);
    }

    private CommunityChatMessage createSystemMessage(
            Long postId, Long userId, String nickname, int profileIcon, CommunityChatSystemEventType event) {
        return CommunityChatMessage.createMessage(
                UUID.randomUUID().toString(),
                postId,
                userId,
                nickname,
                profileIcon,
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
