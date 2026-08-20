package com.team.cops_and_robbers.community.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatSendCommand;
import com.team.cops_and_robbers.community.application.event.CommunityChatMessageSavedEvent;
import com.team.cops_and_robbers.community.domain.CommunityChatGameInviteData;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.community.repository.CommunityChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityChatService {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_MESSAGE_KEY_LENGTH = 36;

    private final CommunityChatMemberRepository communityChatMemberRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void send(CommunityChatSendCommand command) {
        validateMessageType(command.messageType());
        String body = resolveBody(command);
        String messageKey = resolveMessageKey(command.messageKey());

        String senderNickname = communityChatMemberRepository
                .findNicknameByPostIdAndUserId(command.postId(), command.userId())
                .orElseThrow(() -> new ApplicationException(CommunityChatException.NOT_A_CHAT_MEMBER));

        CommunityChatMessage saved = communityChatMessageRepository.save(CommunityChatMessage.createMessage(
                messageKey,
                command.postId(),
                command.userId(),
                senderNickname,
                body,
                command.messageType()
        ));
        eventPublisher.publishEvent(new CommunityChatMessageSavedEvent(saved));
    }

    private void validateMessageType(CommunityChatMessageType messageType) {
        if (messageType == null || messageType == CommunityChatMessageType.SYSTEM) {
            throw new ApplicationException(CommunityChatException.INVALID_MESSAGE_TYPE);
        }
    }

    private String resolveBody(CommunityChatSendCommand command) {
        if (command.messageType() == CommunityChatMessageType.GAME_INVITE) {
            return serialize(validatedInvite(command.gameInvite()));
        }
        return validatedText(command.message());
    }

    private String validatedText(String message) {
        if (!StringUtils.hasText(message)) {
            throw new ApplicationException(CommunityChatException.EMPTY_MESSAGE);
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new ApplicationException(CommunityChatException.MESSAGE_TOO_LONG);
        }
        return message;
    }

    private CommunityChatGameInviteData validatedInvite(CommunityChatGameInviteData invite) {
        if (invite == null
                || !StringUtils.hasText(invite.inviteCode())
                || !StringUtils.hasText(invite.inviterNickname())) {
            throw new ApplicationException(CommunityChatException.INVALID_GAME_INVITE);
        }
        return invite;
    }

    private String serialize(CommunityChatGameInviteData invite) {
        try {
            return objectMapper.writeValueAsString(invite);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(CommunityChatException.INVALID_GAME_INVITE);
        }
    }

    private String resolveMessageKey(String messageKey) {
        if (!StringUtils.hasText(messageKey)) {
            return UUID.randomUUID().toString();
        }
        if (messageKey.length() > MAX_MESSAGE_KEY_LENGTH) {
            throw new ApplicationException(CommunityChatException.INVALID_MESSAGE_KEY);
        }
        return messageKey;
    }
}
