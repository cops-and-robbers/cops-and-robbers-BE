package com.team.cops_and_robbers.community.notification.application;

import com.team.cops_and_robbers.community.comment.application.event.CommunityCommentCreatedEvent;
import com.team.cops_and_robbers.community.comment.domain.CommunityComment;
import com.team.cops_and_robbers.community.notification.application.dto.CommunityNotificationDispatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityNotificationEventHandler {

    private final CommunityNotificationService communityNotificationService;
    private final CommunityFcmNotifier communityFcmNotifier;

    /**
     * 저장이 커밋된 뒤에만 푸시를 보낸다. 저장과 푸시를 한 트랜잭션 안에서 이어 부르면
     * 커밋이 실패해도 푸시는 이미 나가서 "푸시는 왔는데 알림함이 비어 있는" 상태가 된다.
     * 댓글은 이미 커밋된 뒤이므로 알림이 실패해도 댓글에 영향이 없도록 여기서 삼킨다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommunityCommentCreatedEvent event) {
        CommunityComment comment = event.comment();
        try {
            CommunityNotificationDispatch dispatch = communityNotificationService.createNotifications(comment);
            if (dispatch.isEmpty()) {
                return;
            }
            communityFcmNotifier.notifyCommentCreated(dispatch.recipients(), dispatch.push());
        } catch (Exception e) {
            log.error("[Notification] Create failed | commentId={}, postId={}",
                    comment.getId(), comment.getCommunityPostId(), e);
        }
    }
}
