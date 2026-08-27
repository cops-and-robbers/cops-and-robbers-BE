package com.team.cops_and_robbers.community.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatHistoryCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatSendCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityChatHistoryResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityChatMessageResult;
import com.team.cops_and_robbers.community.application.event.CommunityChatMessageSavedEvent;
import com.team.cops_and_robbers.community.domain.CommunityChatGameInviteData;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.community.repository.CommunityChatMessageRepository;
import com.team.cops_and_robbers.community.repository.CommunityChatSenderProfileProjection;
import com.team.cops_and_robbers.user.repository.UserProfileProjection;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityChatService {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_MESSAGE_KEY_LENGTH = 36;

    private final CommunityChatMemberRepository communityChatMemberRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void send(CommunityChatSendCommand command) {
        validateMessageType(command.messageType());
        String body = resolveBody(command);
        String messageKey = resolveMessageKey(command.messageKey());

        CommunityChatSenderProfileProjection sender = communityChatMemberRepository
                .findSenderProfileByPostIdAndUserId(command.postId(), command.userId())
                .orElseThrow(() -> new ApplicationException(CommunityChatException.NOT_A_CHAT_MEMBER));

        CommunityChatMessage saved = communityChatMessageRepository.save(CommunityChatMessage.createMessage(
                messageKey,
                command.postId(),
                command.userId(),
                sender.nickname(),
                sender.profileIcon(),
                body,
                command.messageType()
        ));
        eventPublisher.publishEvent(new CommunityChatMessageSavedEvent(saved));
    }

    /**
     * 커서 페이징으로 최신부터 조회한다.
     * 닉네임은 users의 현재 값을 우선하므로, 페이지에 등장한 발신자만 모아 한 번에 조회한다.
     */
    public CommunityChatHistoryResult getHistory(CommunityChatHistoryCommand command) {
        validateChatMember(command.postId(), command.userId());

        List<CommunityChatMessage> found = communityChatMessageRepository.findPageByPostId(
                command.postId(), command.cursor(), command.toPageable());

        boolean hasNext = found.size() > command.size();
        List<CommunityChatMessage> messages = trimToRequestedSize(found, command.size());
        Map<Long, UserProfileProjection> currentProfiles = findCurrentProfiles(messages);

        List<CommunityChatMessageResult> results = messages.stream()
                .map(message -> CommunityChatMessageResult.of(message, currentProfiles.get(message.getSenderId())))
                .toList();

        return CommunityChatHistoryResult.of(results, hasNext);
    }

    /**
     * 조회 결과에서 요청 크기를 넘는 초과분을 걷어낸다.
     * 다음 페이지 존재 여부를 count 쿼리 없이 알아내려고 한 건 더 조회했기 때문이다.
     * 그 한 건은 hasNext 판단에만 쓰이고 응답에는 포함하지 않는다.
     */
    private List<CommunityChatMessage> trimToRequestedSize(List<CommunityChatMessage> found, int requestedSize) {
        if (found.size() <= requestedSize) {
            return found;
        }
        return found.subList(0, requestedSize);
    }

    private Map<Long, UserProfileProjection> findCurrentProfiles(List<CommunityChatMessage> messages) {
        Set<Long> senderIds = messages.stream()
                .map(CommunityChatMessage::getSenderId)
                .collect(Collectors.toSet());

        if (senderIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findProfilesByIds(senderIds).stream()
                .collect(Collectors.toMap(UserProfileProjection::userId, Function.identity()));
    }

    private void validateChatMember(Long postId, Long userId) {
        if (!communityChatMemberRepository.existsByCommunityPostIdAndUserId(postId, userId)) {
            throw new ApplicationException(CommunityChatException.NOT_A_CHAT_MEMBER);
        }
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
        if (invite == null || !StringUtils.hasText(invite.inviteCode())) {
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
