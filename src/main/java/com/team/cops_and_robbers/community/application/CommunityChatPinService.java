package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatPinDeleteCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatPinRegisterCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatPinUpdateCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityChatPinResult;
import com.team.cops_and_robbers.community.application.event.CommunityChatMessageSavedEvent;
import com.team.cops_and_robbers.community.application.event.CommunityChatPinChangedEvent;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatPin;
import com.team.cops_and_robbers.community.domain.CommunityChatSystemEventType;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.community.repository.CommunityChatMessageRepository;
import com.team.cops_and_robbers.community.repository.CommunityChatPinRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserProfileProjection;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityChatPinService {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityChatMemberRepository communityChatMemberRepository;
    private final CommunityChatPinRepository communityChatPinRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;
    private final CommunityChatSystemMessageFactory communityChatSystemMessageFactory;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 방마다 고정 채팅은 최대 1개
     * 재등록은 이력 없이 이전 것을 지우고 새로 만든다.
     */
    @Transactional
    public CommunityChatPinResult register(CommunityChatPinRegisterCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateHost(post, command.userId());

        communityChatPinRepository.deleteByCommunityPostId(command.postId());
        CommunityChatPin pin = communityChatPinRepository.save(
                CommunityChatPin.createPin(command.postId(), command.userId(), command.content()));

        User host = userRepository.getByUserId(command.userId());
        CommunityChatMessage systemMessage = communityChatMessageRepository.save(
                communityChatSystemMessageFactory.createPinRegisteredMessage(command.postId(), host));
        publishEvents(pin, CommunityChatSystemEventType.PIN_REGISTERED, host.getId(), systemMessage);

        return CommunityChatPinResult.of(pin, host.getNickname(), host.getProfileIcon());
    }

    @Transactional
    public CommunityChatPinResult update(CommunityChatPinUpdateCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateHost(post, command.userId());

        CommunityChatPin pin = getPin(command.postId());
        pin.updateContent(command.content());

        User host = userRepository.getByUserId(command.userId());
        CommunityChatMessage systemMessage = communityChatMessageRepository.save(
                communityChatSystemMessageFactory.createPinUpdatedMessage(command.postId(), host));
        publishEvents(pin, CommunityChatSystemEventType.PIN_UPDATED, host.getId(), systemMessage);

        return CommunityChatPinResult.of(pin, host.getNickname(), host.getProfileIcon());
    }

    @Transactional
    public void delete(CommunityChatPinDeleteCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateHost(post, command.userId());

        CommunityChatPin pin = getPin(command.postId());
        User host = userRepository.getByUserId(command.userId());
        CommunityChatMessage systemMessage = communityChatMessageRepository.save(
                communityChatSystemMessageFactory.createPinDeletedMessage(command.postId(), host));
        publishEvents(pin, CommunityChatSystemEventType.PIN_DELETED, host.getId(), systemMessage);

        communityChatPinRepository.delete(pin);
    }

    public CommunityChatPinResult get(Long postId, Long userId) {
        validateChatMember(postId, userId);

        return communityChatPinRepository.findByCommunityPostId(postId)
                .map(pin -> {
                    User writer = userRepository.findById(pin.getWriterId()).orElse(null);
                    UserProfileProjection writerProfile = UserProfileProjection.of(pin.getWriterId(), writer);

                    return CommunityChatPinResult.of(pin, writerProfile.nickname(), writerProfile.profileIcon());
                })
                .orElseGet(() -> CommunityChatPinResult.empty(postId));
    }

    private void publishEvents(
        CommunityChatPin pin, CommunityChatSystemEventType action, Long actorId, CommunityChatMessage systemMessage
    ) {
        eventPublisher.publishEvent(new CommunityChatMessageSavedEvent(systemMessage));
        eventPublisher.publishEvent(new CommunityChatPinChangedEvent(pin, action, actorId));
    }

    private CommunityChatPin getPin(Long postId) {
        return communityChatPinRepository.findByCommunityPostId(postId)
                .orElseThrow(() -> new ApplicationException(CommunityChatException.CHAT_PIN_NOT_FOUND));
    }

    private void validateChatMember(Long postId, Long userId) {
        if (!communityChatMemberRepository.existsByCommunityPostIdAndUserId(postId, userId)) {
            throw new ApplicationException(CommunityChatException.NOT_A_CHAT_MEMBER);
        }
    }

    private void validateHost(CommunityPost post, Long userId) {
        if (!post.getWriterId().equals(userId)) {
            throw new ApplicationException(CommunityChatException.FORBIDDEN_NOT_CHAT_PIN_HOST);
        }
    }
}
