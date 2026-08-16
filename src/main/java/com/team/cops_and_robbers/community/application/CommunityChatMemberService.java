package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatJoinCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatLeaveCommand;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.community.repository.CommunityChatMessageRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityChatMemberService {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityChatMemberRepository communityChatMemberRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;
    private final CommunityChatSystemMessageFactory communityChatSystemMessageFactory;
    private final UserRepository userRepository;

    @Transactional
    public void join(CommunityChatJoinCommand command) {
        CommunityPost post = communityPostRepository.getByPostIdForUpdate(command.postId());
        validateRecruiting(post);
        validateNotJoined(command.postId(), command.userId());
        validateCapacity(post);

        User user = userRepository.getByUserId(command.userId());
        communityChatMemberRepository.save(CommunityChatMember.createMember(command.postId(), command.userId()));
        communityChatMessageRepository.save(
                communityChatSystemMessageFactory.createJoinMessage(command.postId(), user));
    }

    @Transactional
    public void leave(CommunityChatLeaveCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateNotAuthor(post, command.userId());

        CommunityChatMember member = communityChatMemberRepository
                .findByCommunityPostIdAndUserId(command.postId(), command.userId())
                .orElseThrow(() -> new ApplicationException(CommunityChatException.NOT_A_CHAT_MEMBER));

        User user = userRepository.getByUserId(command.userId());
        communityChatMessageRepository.save(
                communityChatSystemMessageFactory.createLeaveMessage(command.postId(), user));
        communityChatMemberRepository.delete(member);
    }

    private void validateRecruiting(CommunityPost post) {
        if (post.getStatus() != RecruitmentStatus.RECRUITING) {
            throw new ApplicationException(CommunityChatException.RECRUITMENT_CLOSED);
        }
    }

    private void validateNotJoined(Long postId, Long userId) {
        if (communityChatMemberRepository.existsByCommunityPostIdAndUserId(postId, userId)) {
            throw new ApplicationException(CommunityChatException.ALREADY_JOINED);
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