package com.team.cops_and_robbers.community.notification.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.community.comment.application.event.CommunityCommentCreatedEvent;
import com.team.cops_and_robbers.community.comment.domain.CommunityComment;
import com.team.cops_and_robbers.community.notification.application.dto.CommunityNotificationDispatch;
import com.team.cops_and_robbers.community.notification.application.dto.CommunityNotificationPush;
import com.team.cops_and_robbers.community.notification.domain.CommunityNotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

class CommunityNotificationEventHandlerTest extends ServiceUnitTest {

    private static final Long POST_ID = 1L;
    private static final Long POST_WRITER_ID = 100L;
    private static final Long COMMENTER_ID = 200L;
    private static final Long THIRD_PARTY_ID = 400L;

    @InjectMocks
    private CommunityNotificationEventHandler communityNotificationEventHandler;

    @Mock
    private CommunityNotificationService communityNotificationService;

    @Mock
    private CommunityFcmNotifier communityFcmNotifier;

    private CommunityComment comment(Long id, Long parentId, Long writerId) {
        CommunityComment comment = CommunityComment.createComment(POST_ID, parentId, writerId, "몇 시에 만나나요?");
        setId(comment, id);
        return comment;
    }

    private CommunityNotificationDispatch dispatch(List<Long> recipients, CommunityNotificationType type) {
        return new CommunityNotificationDispatch(recipients,
                new CommunityNotificationPush(type, POST_ID, "같이 하실 분!", "몇 시에 만나나요?"));
    }

    private void handle(CommunityComment comment) {
        communityNotificationEventHandler.handleCommentCreated(new CommunityCommentCreatedEvent(comment));
    }

    private CommunityNotificationPush capturePush() {
        ArgumentCaptor<CommunityNotificationPush> captor =
                ArgumentCaptor.forClass(CommunityNotificationPush.class);
        then(communityFcmNotifier).should().notifyCommentCreated(any(), captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("댓글 생성 이벤트 처리")
    class HandleCommentCreated {

        @Test
        void 저장된_수신자와_같은_사람들에게_푸시를_보낸다() {
            CommunityComment comment = comment(10L, null, COMMENTER_ID);
            given(communityNotificationService.createNotifications(comment))
                    .willReturn(dispatch(List.of(POST_WRITER_ID, THIRD_PARTY_ID), CommunityNotificationType.COMMENT));

            handle(comment);

            ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
            then(communityFcmNotifier).should().notifyCommentCreated(captor.capture(), any());
            assertThat(captor.getValue()).containsExactly(POST_WRITER_ID, THIRD_PARTY_ID);
        }

        @Test
        void 알림이_저장된_뒤에_푸시를_보낸다() {
            CommunityComment comment = comment(10L, null, COMMENTER_ID);
            given(communityNotificationService.createNotifications(comment))
                    .willReturn(dispatch(List.of(POST_WRITER_ID), CommunityNotificationType.COMMENT));

            handle(comment);

            InOrder inOrder = inOrder(communityNotificationService, communityFcmNotifier);
            inOrder.verify(communityNotificationService).createNotifications(comment);
            inOrder.verify(communityFcmNotifier).notifyCommentCreated(any(), any());
        }

        @Test
        void 루트_댓글이면_댓글_종류로_푸시_내용을_만든다() {
            CommunityComment comment = comment(10L, null, COMMENTER_ID);
            given(communityNotificationService.createNotifications(comment))
                    .willReturn(dispatch(List.of(POST_WRITER_ID), CommunityNotificationType.COMMENT));

            handle(comment);

            CommunityNotificationPush push = capturePush();
            assertSoftly(softly -> {
                softly.assertThat(push.type()).isEqualTo(CommunityNotificationType.COMMENT);
                softly.assertThat(push.communityPostId()).isEqualTo(POST_ID);
                softly.assertThat(push.content()).isEqualTo("몇 시에 만나나요?");
            });
        }

        @Test
        void 답글이면_답글_종류로_푸시_내용을_만든다() {
            CommunityComment reply = comment(11L, 10L, COMMENTER_ID);
            given(communityNotificationService.createNotifications(reply))
                    .willReturn(dispatch(List.of(POST_WRITER_ID), CommunityNotificationType.REPLY));

            handle(reply);

            assertThat(capturePush().type()).isEqualTo(CommunityNotificationType.REPLY);
        }

        @Test
        void 받을_사람이_없으면_푸시를_보내지_않는다() {
            CommunityComment comment = comment(10L, null, COMMENTER_ID);
            given(communityNotificationService.createNotifications(comment)).willReturn(CommunityNotificationDispatch.none());

            handle(comment);

            then(communityFcmNotifier).should(never()).notifyCommentCreated(any(), any());
        }

        @Test
        void 알림_저장이_실패하면_푸시도_보내지_않고_예외를_밖으로_던지지_않는다() {
            CommunityComment comment = comment(10L, null, COMMENTER_ID);
            given(communityNotificationService.createNotifications(comment))
                    .willThrow(new IllegalStateException("boom"));

            handle(comment);

            then(communityFcmNotifier).should(never()).notifyCommentCreated(any(), any());
        }
    }
}
