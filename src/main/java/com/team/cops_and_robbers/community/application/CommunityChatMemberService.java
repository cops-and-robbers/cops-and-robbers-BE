package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatJoinCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatLeaveCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityChatRoomResult;
import com.team.cops_and_robbers.community.application.event.CommunityChatMessageSavedEvent;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberCountProjection;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.community.repository.CommunityChatMessageRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityChatMemberService {

    private static final int MAX_JOINED_ROOMS = 100;

    private static final Comparator<CommunityChatRoomResult> RECENT_CHAT_FIRST =
            Comparator.comparing(
                    CommunityChatMemberService::lastChatAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );

    private final CommunityPostRepository communityPostRepository;
    private final CommunityChatMemberRepository communityChatMemberRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;
    private final CommunityChatSystemMessageFactory communityChatSystemMessageFactory;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void join(CommunityChatJoinCommand command) {
        CommunityPost post = communityPostRepository.getByPostIdForUpdate(command.postId());
        validateRecruiting(post);
        validateNotJoined(command.postId(), command.userId());
        validateJoinedRoomLimit(command.userId());
        validateCapacity(post);

        User user = userRepository.getByUserId(command.userId());
        communityChatMemberRepository.save(CommunityChatMember.createMember(command.postId(), command.userId()));

        CommunityChatMessage systemMessage = communityChatMessageRepository.save(
                communityChatSystemMessageFactory.createJoinMessage(command.postId(), user));
        eventPublisher.publishEvent(new CommunityChatMessageSavedEvent(systemMessage));
    }

    @Transactional
    public void leave(CommunityChatLeaveCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateNotAuthor(post, command.userId());

        CommunityChatMember member = communityChatMemberRepository
                .findByCommunityPostIdAndUserId(command.postId(), command.userId())
                .orElseThrow(() -> new ApplicationException(CommunityChatException.NOT_A_CHAT_MEMBER));

        User user = userRepository.getByUserId(command.userId());
        CommunityChatMessage systemMessage = communityChatMessageRepository.save(
                communityChatSystemMessageFactory.createLeaveMessage(command.postId(), user));
        communityChatMemberRepository.delete(member);

        eventPublisher.publishEvent(new CommunityChatMessageSavedEvent(systemMessage));
    }

    /**
     * 참여 방 수에 상한이 있어 전체를 한 번에 반환한다.
     * 마지막 대화가 최근인 방부터, 대화가 없는 방은 뒤로 보낸다.
     */
    public List<CommunityChatRoomResult> getChatRooms(Long userId) {
        List<Long> postIds = communityChatMemberRepository.findPostIdsByUserId(userId);
        if (postIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> memberCounts = findMemberCounts(postIds);
        Map<Long, CommunityChatMessage> lastMessages = findLastMessages(postIds);

        return communityPostRepository.findAllById(postIds).stream()
                .map(post -> CommunityChatRoomResult.of(
                        post,
                        memberCounts.getOrDefault(post.getId(), 0L),
                        lastMessages.get(post.getId())))
                .sorted(RECENT_CHAT_FIRST)
                .toList();
    }

    private Map<Long, Long> findMemberCounts(List<Long> postIds) {
        return communityChatMemberRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(
                        CommunityChatMemberCountProjection::postId,
                        CommunityChatMemberCountProjection::count));
    }

    private Map<Long, CommunityChatMessage> findLastMessages(List<Long> postIds) {
        return communityChatMessageRepository.findLatestByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(
                        CommunityChatMessage::getCommunityPostId,
                        Function.identity()));
    }

    /**
     * 아직 대화가 없는 방은 null을 반환한다. 정렬에서 nullsLast로 처리된다.
     */
    private static String lastChatAt(CommunityChatRoomResult room) {
        return room.lastMessage() == null ? null : room.lastMessage().createdAt();
    }

    private void validateRecruiting(CommunityPost post) {
        if (post.isClosed()) {
            throw new ApplicationException(CommunityChatException.RECRUITMENT_CLOSED);
        }
    }

    private void validateNotJoined(Long postId, Long userId) {
        if (communityChatMemberRepository.existsByCommunityPostIdAndUserId(postId, userId)) {
            throw new ApplicationException(CommunityChatException.ALREADY_JOINED);
        }
    }

    /**
     * 게시글 작성 시의 자동 등록은 채팅방 상한(100개)을 적용하지 않는다.
     * 자기 글의 채팅방에는 항상 들어갈 수 있어야 하기 때문이다.
     */
    private void validateJoinedRoomLimit(Long userId) {
        if (communityChatMemberRepository.countByUserId(userId) >= MAX_JOINED_ROOMS) {
            throw new ApplicationException(CommunityChatException.JOINED_CHAT_ROOM_LIMIT_EXCEEDED);
        }
    }

    private void validateCapacity(CommunityPost post) {
        int currentCount = communityChatMemberRepository.countByCommunityPostId(post.getId());
        if (currentCount >= post.getMaxParticipants()) {
            throw new ApplicationException(CommunityChatException.CHAT_ROOM_FULL);
        }
    }

    private void validateNotAuthor(CommunityPost post, Long userId) {
        if (post.getWriterId().equals(userId)) {
            throw new ApplicationException(CommunityChatException.AUTHOR_CANNOT_LEAVE);
        }
    }
}
