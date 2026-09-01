package com.team.cops_and_robbers.community.comment.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.comment.application.dto.command.CommunityCommentCreateCommand;
import com.team.cops_and_robbers.community.comment.application.dto.command.CommunityCommentDeleteCommand;
import com.team.cops_and_robbers.community.comment.application.dto.command.CommunityCommentListCommand;
import com.team.cops_and_robbers.community.comment.application.dto.command.CommunityCommentNotificationCommand;
import com.team.cops_and_robbers.community.comment.application.dto.result.CommunityCommentListResult;
import com.team.cops_and_robbers.community.comment.application.dto.result.CommunityCommentResult;
import com.team.cops_and_robbers.community.comment.application.event.CommunityCommentCreatedEvent;
import com.team.cops_and_robbers.community.comment.domain.CommunityComment;
import com.team.cops_and_robbers.community.comment.exception.CommunityCommentException;
import com.team.cops_and_robbers.community.comment.repository.CommunityCommentRepository;
import com.team.cops_and_robbers.community.post.repository.CommunityPostRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityCommentService {

    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostRepository communityPostRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommunityCommentResult createComment(CommunityCommentCreateCommand command) {
        User writer = userRepository.getByUserId(command.writerId());
        communityPostRepository.getByPostId(command.postId());
        validateParent(command);

        CommunityComment saved = communityCommentRepository.save(
                CommunityComment.createComment(
                        command.postId(),
                        command.parentId(),
                        command.writerId(),
                        command.content()
                )
        );
        eventPublisher.publishEvent(new CommunityCommentCreatedEvent(saved));

        return CommunityCommentResult.from(saved, writer);
    }

    /**
     * 답글까지 한 화면에 묶어 내려주기 위해 두 단계로 조회
     * 1) size+1개를 커서로 가져와 마지막 한 건으로 다음 페이지 존재 여부를 판단
     * 2) 그렇게 확정된 루트 댓글들의 답글을 부모 id로 한 번에 모아오고, 작성자 닉네임도 배치로 조회해 N+1을 피한다.
     */
    public CommunityCommentListResult getComments(CommunityCommentListCommand command) {
        communityPostRepository.getByPostId(command.postId());

        // 루트 댓글을 cursor 기반으로 size + 1개 조회
        List<CommunityComment> fetched = communityCommentRepository.findRootPageByCursor(
                command.postId(),
                command.cursor(),
                command.toPageable()
        );

        boolean hasNext = fetched.size() > command.size();
        List<CommunityComment> roots = hasNext ? fetched.subList(0, command.size()) : fetched;

        // 루트 댓글 기준으로 대댓글 조회 + 작성자를 한 번에 조회 (N+1 방지)
        List<CommunityComment> replies = findReplies(roots);
        Map<Long, User> writers = findWriters(Stream.concat(roots.stream(), replies.stream()).toList());
        Map<Long, List<CommunityComment>> repliesByParent = groupRepliesByParent(replies);

        List<CommunityCommentResult> content = roots.stream()
                .map(root -> toResultWithReplies(root, repliesByParent, writers))
                .toList();

        Long nextCursor = hasNext ? roots.getLast().getId() : null;

        return new CommunityCommentListResult(content, nextCursor, hasNext);
    }

    @Transactional
    public void updateReplyNotificationsEnabled(CommunityCommentNotificationCommand command) {
        CommunityComment comment = communityCommentRepository.getByCommentId(command.commentId());
        if (!comment.isWrittenBy(command.writerId())) {
            throw new ApplicationException(CommunityCommentException.FORBIDDEN_NOT_COMMENT_AUTHOR);
        }
        comment.updateReplyNotificationsEnabled(command.replyNotificationsEnabled());
    }

    @Transactional
    public void deleteComment(CommunityCommentDeleteCommand command) {
        CommunityComment comment = communityCommentRepository.getByCommentId(command.commentId());
        if (!comment.isWrittenBy(command.writerId())) {
            throw new ApplicationException(CommunityCommentException.FORBIDDEN_NOT_COMMENT_AUTHOR);
        }

        if (comment.isReply()) {
            deleteReply(comment);
            return;
        }
        deleteRoot(comment);
    }

    /**
     * 답글이 남아 있으면 삭제 문구만 표시하고, 마지막 답글이 지워질 때 껍데기 부모를 함께 정리한다.
     */
    private void deleteRoot(CommunityComment root) {
        CommunityComment locked = communityCommentRepository.getByCommentIdForUpdate(root.getId());
        if (communityCommentRepository.countByParentId(locked.getId()) > 0) {
            locked.markDeleted();
            return;
        }
        communityCommentRepository.delete(locked);
    }

    /**
     * 부모를 먼저 잠근 뒤 답글을 지운다.
     * 락 순서 : 항상 부모 -> 자식으로 고정
     */
    private void deleteReply(CommunityComment reply) {
        CommunityComment parent = communityCommentRepository.getByCommentIdForUpdate(reply.getParentId());
        boolean lastReply = communityCommentRepository.countByParentId(parent.getId()) == 1;

        communityCommentRepository.delete(reply);
        if (parent.isDeleted() && lastReply) {
            communityCommentRepository.delete(parent);
        }
    }

    private void validateParent(CommunityCommentCreateCommand command) {
        if (command.parentId() == null) {
            return;
        }
        CommunityComment parent = communityCommentRepository.findByCommentIdForUpdate(command.parentId())
                .orElseThrow(() -> new ApplicationException(CommunityCommentException.PARENT_COMMENT_NOT_FOUND));

        if (!parent.getCommunityPostId().equals(command.postId())) {
            throw new ApplicationException(CommunityCommentException.PARENT_COMMENT_POST_MISMATCH);
        }
        if (parent.isReply()) {
            throw new ApplicationException(CommunityCommentException.INVALID_COMMENT_DEPTH);
        }
        if (parent.isDeleted()) {
            throw new ApplicationException(CommunityCommentException.DELETED_COMMENT_CANNOT_REPLY);
        }
    }

    private List<CommunityComment> findReplies(List<CommunityComment> roots) {
        if (roots.isEmpty()) {
            return List.of();
        }
        return communityCommentRepository.findRepliesByParentIds(
                roots.stream().map(CommunityComment::getId).toList());
    }

    private Map<Long, List<CommunityComment>> groupRepliesByParent(List<CommunityComment> replies) {
        return replies.stream().collect(Collectors.groupingBy(CommunityComment::getParentId));
    }

    private CommunityCommentResult toResultWithReplies(
            CommunityComment root,
            Map<Long, List<CommunityComment>> repliesByParent,
            Map<Long, User> writers
    ) {
        List<CommunityCommentResult> replies = repliesByParent.getOrDefault(root.getId(), List.of()).stream()
                .map(reply -> CommunityCommentResult.from(reply, writers.get(reply.getWriterId())))
                .toList();
        return CommunityCommentResult.of(root, writers.get(root.getWriterId()), replies);
    }

    private Map<Long, User> findWriters(List<CommunityComment> comments) {
        List<Long> writerIds = comments.stream()
                .map(CommunityComment::getWriterId)
                .distinct()
                .toList();

        return userRepository.findAllById(writerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
