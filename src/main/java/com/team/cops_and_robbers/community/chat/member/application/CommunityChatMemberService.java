package com.team.cops_and_robbers.community.chat.member.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.chat.common.application.CommunityChatSystemMessageFactory;
import com.team.cops_and_robbers.community.chat.common.application.event.CommunityChatMessageSavedEvent;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.chat.common.exception.CommunityChatException;
import com.team.cops_and_robbers.community.chat.common.repository.CommunityChatMessageRepository;
import com.team.cops_and_robbers.community.chat.member.application.dto.command.CommunityChatJoinCommand;
import com.team.cops_and_robbers.community.chat.member.application.dto.command.CommunityChatKickCommand;
import com.team.cops_and_robbers.community.chat.member.application.dto.command.CommunityChatLeaveCommand;
import com.team.cops_and_robbers.community.chat.member.application.dto.command.CommunityChatNotificationCommand;
import com.team.cops_and_robbers.community.chat.member.application.dto.command.CommunityChatReadCommand;
import com.team.cops_and_robbers.community.chat.member.application.dto.result.CommunityChatMemberListResult;
import com.team.cops_and_robbers.community.chat.member.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.chat.member.repository.CommunityChatMemberCountProjection;
import com.team.cops_and_robbers.community.chat.member.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.community.chat.message.application.dto.CommunityChatRoomListContext;
import com.team.cops_and_robbers.community.chat.message.application.dto.result.CommunityChatRoomResult;
import com.team.cops_and_robbers.community.post.domain.CommunityPost;
import com.team.cops_and_robbers.community.post.repository.CommunityPostRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserProfileProjection;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * 방장만 멤버를 강퇴할 수 있다. 강퇴된 유저는 재입장 제한 없이 다시 참여할 수 있다.
     */
    @Transactional
    public void kick(CommunityChatKickCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateHost(post, command.hostId());
        validateNotSelf(command.hostId(), command.targetUserId());

        CommunityChatMember target = communityChatMemberRepository
                .findByCommunityPostIdAndUserId(command.postId(), command.targetUserId())
                .orElseThrow(() -> new ApplicationException(CommunityChatException.CHAT_MEMBER_NOT_FOUND));

        User targetUser = userRepository.findById(command.targetUserId()).orElse(null);
        UserProfileProjection targetProfile = UserProfileProjection.of(command.targetUserId(), targetUser);

        CommunityChatMessage systemMessage = communityChatMessageRepository.save(
                communityChatSystemMessageFactory.createKickMessage(
                        command.postId(), targetProfile.userId(), targetProfile.nickname(), targetProfile.profileIcon()));

        communityChatMemberRepository.delete(target);

        eventPublisher.publishEvent(new CommunityChatMessageSavedEvent(systemMessage));
    }

    /**
     * 참여 방 수에 상한이 있어 전체를 한 번에 반환한다.
     * 마지막 대화가 최근인 방부터, 대화가 없는 방은 뒤로 보낸다.
     */
    public List<CommunityChatRoomResult> getChatRooms(Long userId) {
        List<CommunityChatMember> members = communityChatMemberRepository.findAllByUserId(userId);
        if (members.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = members.stream().map(CommunityChatMember::getCommunityPostId).toList();
        Map<Long, CommunityChatMessage> lastMessages = findLastMessages(postIds);
        CommunityChatRoomListContext context = CommunityChatRoomListContext.of(
                findMemberCounts(postIds),
                lastMessages,
                findCurrentSenderProfiles(lastMessages.values()),
                findUnreadCounts(userId)
        );

        return communityPostRepository.findAllById(postIds).stream()
                .map(post -> toRoomResult(post, context))
                .sorted(RECENT_CHAT_FIRST)
                .toList();
    }

    private CommunityChatRoomResult toRoomResult(CommunityPost post, CommunityChatRoomListContext context) {
        CommunityChatMessage lastMessage = context.lastMessages().get(post.getId());
        UserProfileProjection senderProfile =
                (lastMessage == null) ? null : context.senderProfiles().get(lastMessage.getSenderId());

        return CommunityChatRoomResult.of(
                post,
                context.memberCounts().getOrDefault(post.getId(), 0L),
                lastMessage,
                senderProfile,
                context.unreadCounts().getOrDefault(post.getId(), 0L));
    }

    public CommunityChatMemberListResult getMembers(Long postId, Long requesterId) {
        validateChatMember(postId, requesterId);

        List<CommunityChatMember> members = communityChatMemberRepository.findAllByCommunityPostId(postId);
        Long writerId = communityPostRepository.getByPostId(postId).getWriterId();
        Map<Long, User> users = findUsers(members);

        List<CommunityChatMemberListResult.Member> results = members.stream()
                .map(member -> CommunityChatMemberListResult.Member.of(
                                member.getUserId(), users.get(member.getUserId()), writerId))
                .toList();
        return new CommunityChatMemberListResult(notificationEnabled(members, requesterId), results);
    }

    private boolean notificationEnabled(List<CommunityChatMember> members, Long requesterId) {
        return members.stream()
                .filter(member -> member.getUserId().equals(requesterId))
                .findFirst()
                .map(CommunityChatMember::isAllowNotification)
                .orElse(true);
    }

    private void validateChatMember(Long postId, Long userId) {
        if (!communityChatMemberRepository.existsByCommunityPostIdAndUserId(postId, userId)) {
            throw new ApplicationException(CommunityChatException.NOT_A_CHAT_MEMBER);
        }
    }

    private Map<Long, User> findUsers(List<CommunityChatMember> members) {
        List<Long> userIds = members.stream().map(CommunityChatMember::getUserId).distinct().toList();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    @Transactional
    public void updateNotification(CommunityChatNotificationCommand command) {
        getMember(command.postId(), command.userId()).updateNotification(command.allowNotification());
    }

    /**
     * 읽음 위치는 앞으로만 가고, 방의 마지막 메시지를 넘지 않는다.
     * 오래된 id는 뒤로 밀지 않고, 더 큰 id는 앞으로 올 메시지까지 읽은 것이 되어 미읽음이 0에서 움직이지 않는다.
     */
    @Transactional
    public void read(CommunityChatReadCommand command) {
        CommunityChatMember member = getMember(command.postId(), command.userId());
        Long latestMessageId = communityChatMessageRepository
                .findLatestMessageIdByPostId(command.postId())
                .orElse(null);
        if (latestMessageId == null) {
            return;
        }

        Long readUpTo = Math.min(command.lastReadMessageId(), latestMessageId);
        if (member.hasReadUpTo(readUpTo)) {
            return;
        }
        member.readUntil(readUpTo);
    }

    private CommunityChatMember getMember(Long postId, Long userId) {
        return communityChatMemberRepository.findByCommunityPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new ApplicationException(CommunityChatException.NOT_A_CHAT_MEMBER));
    }

    private Map<Long, Long> findMemberCounts(List<Long> postIds) {
        return communityChatMemberRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(
                        CommunityChatMemberCountProjection::postId,
                        CommunityChatMemberCountProjection::count));
    }

    private Map<Long, Long> findUnreadCounts(Long userId) {
        return communityChatMemberRepository.countUnreadByUserId(userId).stream()
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

    private Map<Long, UserProfileProjection> findCurrentSenderProfiles(Collection<CommunityChatMessage> lastMessages) {
        Set<Long> senderIds = lastMessages.stream()
                .map(CommunityChatMessage::getSenderId)
                .collect(Collectors.toSet());

        if (senderIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findProfilesByIds(senderIds).stream()
                .collect(Collectors.toMap(UserProfileProjection::userId, Function.identity()));
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

    private void validateHost(CommunityPost post, Long userId) {
        if (!post.getWriterId().equals(userId)) {
            throw new ApplicationException(CommunityChatException.FORBIDDEN_NOT_CHAT_HOST);
        }
    }

    private void validateNotSelf(Long hostId, Long targetUserId) {
        if (hostId.equals(targetUserId)) {
            throw new ApplicationException(CommunityChatException.CANNOT_KICK_SELF);
        }
    }
}
