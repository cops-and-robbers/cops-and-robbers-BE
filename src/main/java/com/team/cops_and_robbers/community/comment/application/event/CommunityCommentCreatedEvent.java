package com.team.cops_and_robbers.community.comment.application.event;

import com.team.cops_and_robbers.community.comment.domain.CommunityComment;

/**
 * 댓글 알림은 별도 PR에서 붙인다. 여기서 이벤트만 먼저 띄워 두면
 * 알림 쪽은 {@code @TransactionalEventListener(AFTER_COMMIT)} 하나만 추가하면 되고,
 * 댓글 저장 트랜잭션은 알림 실패에 영향을 받지 않는다.
 */
public record CommunityCommentCreatedEvent(CommunityComment comment) {
}
