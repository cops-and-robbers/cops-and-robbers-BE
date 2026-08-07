package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostCreateCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostDeleteCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostListCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostStatusCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostUpdateCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.repository.CommunityPostRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPostService {

    private final CommunityPostRepository communityPostRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommunityPostResult createPost(CommunityPostCreateCommand command) {
        userRepository.getByUserId(command.userId());
        validateMeetingDate(command.meetingAt());
        CommunityPost post = communityPostRepository.save(CommunityPost.createPost(command));
        return CommunityPostResult.from(post);
    }

    public Page<CommunityPostResult> getPostList(CommunityPostListCommand command) {
        return communityPostRepository.findAllByOrderByCreatedAtDesc(command.toPageable())
                .map(CommunityPostResult::from);
    }

    public CommunityPostResult getPost(Long postId) {
        return CommunityPostResult.from(communityPostRepository.getByPostId(postId));
    }

    @Transactional
    public CommunityPostResult updatePost(CommunityPostUpdateCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateAuthor(post, command.userId());
        validateMeetingDate(command.meetingAt());
        post.updatePost(command);
        return CommunityPostResult.from(post);
    }

    @Transactional
    public void deletePost(CommunityPostDeleteCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateAuthor(post, command.userId());
        communityPostRepository.deleteByPostId(command.postId());
    }

    @Transactional
    public CommunityPostResult updateStatus(CommunityPostStatusCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateAuthor(post, command.userId());
        post.updateStatus(command.status());
        return CommunityPostResult.from(post);
    }

    private void validateAuthor(CommunityPost post, Long userId) {
        if (!post.getUserId().equals(userId)) {
            throw new ApplicationException(CommunityPostException.FORBIDDEN_NOT_AUTHOR);
        }
    }

    private void validateMeetingDate(LocalDateTime meetingAt) {
        if (meetingAt.isBefore(LocalDateTime.now())) {
            throw new ApplicationException(CommunityPostException.INVALID_MEETING_DATE);
        }
    }
}
