package com.team.cops_and_robbers.community.notification.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fcm.FcmMessage;
import com.team.cops_and_robbers.common.fcm.FcmService;
import com.team.cops_and_robbers.common.fixture.UserDeviceFixture;
import com.team.cops_and_robbers.community.notification.application.dto.CommunityNotificationPush;
import com.team.cops_and_robbers.community.notification.domain.CommunityNotificationType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class CommunityFcmNotifierTest extends ServiceUnitTest {

    private static final Long POST_ID = 1L;
    private static final Long RECEIVER_ID = 100L;

    @InjectMocks
    private CommunityFcmNotifier communityFcmNotifier;

    @Mock
    private FcmService fcmService;

    private UserDevice deviceOf(Long userId, String nickname, boolean allowCommunityPush) {
        User user = USER(nickname);
        setId(user, userId);
        user.updateCommunityPush(allowCommunityPush);
        return UserDeviceFixture.IOS_DEVICE(user);
    }

    private CommunityNotificationPush push(CommunityNotificationType type) {
        return new CommunityNotificationPush(type, POST_ID, "같이 하실 분!", "몇 시에 만나나요?");
    }

    private FcmMessage captureSent() {
        ArgumentCaptor<FcmMessage> captor = ArgumentCaptor.forClass(FcmMessage.class);
        then(fcmService).should().send(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("커뮤니티 알림 푸시 발송")
    class NotifyCommentCreated {

        @Test
        void 수신을_켠_사람의_토큰으로_발송한다() {
            given(userDeviceRepository.findByUser_IdIn(List.of(RECEIVER_ID)))
                    .willReturn(List.of(deviceOf(RECEIVER_ID, "받는사람", true)));

            communityFcmNotifier.notifyCommentCreated(List.of(RECEIVER_ID), push(CommunityNotificationType.COMMENT));

            FcmMessage sent = captureSent();
            assertSoftly(softly -> {
                softly.assertThat(sent.tokens()).containsExactly("fcm-123");
                softly.assertThat(sent.title()).isEqualTo("같이 하실 분!");
                softly.assertThat(sent.body()).isEqualTo("몇 시에 만나나요?");
                softly.assertThat(sent.data()).containsEntry("type", "COMMENT");
                softly.assertThat(sent.data()).containsEntry("postId", "1");
            });
        }

        @Test
        void 답글도_게시글_제목으로_보내고_종류는_data로_구분한다() {
            given(userDeviceRepository.findByUser_IdIn(List.of(RECEIVER_ID)))
                    .willReturn(List.of(deviceOf(RECEIVER_ID, "받는사람", true)));

            communityFcmNotifier.notifyCommentCreated(List.of(RECEIVER_ID), push(CommunityNotificationType.REPLY));

            FcmMessage sent = captureSent();
            assertSoftly(softly -> {
                softly.assertThat(sent.title()).isEqualTo("같이 하실 분!");
                softly.assertThat(sent.data()).containsEntry("type", "REPLY");
            });
        }

        @Test
        void 수신을_끈_사람에게는_보내지_않는다() {
            given(userDeviceRepository.findByUser_IdIn(List.of(RECEIVER_ID)))
                    .willReturn(List.of(deviceOf(RECEIVER_ID, "받는사람", false)));

            communityFcmNotifier.notifyCommentCreated(List.of(RECEIVER_ID), push(CommunityNotificationType.COMMENT));

            then(fcmService).should(never()).send(any());
        }

        @Test
        void 등록된_기기가_없으면_보내지_않는다() {
            given(userDeviceRepository.findByUser_IdIn(List.of(RECEIVER_ID))).willReturn(List.of());

            communityFcmNotifier.notifyCommentCreated(List.of(RECEIVER_ID), push(CommunityNotificationType.COMMENT));

            then(fcmService).should(never()).send(any());
        }

        @Test
        void 발송에_실패해도_예외를_밖으로_던지지_않는다() {
            given(userDeviceRepository.findByUser_IdIn(List.of(RECEIVER_ID)))
                    .willThrow(new IllegalStateException("boom"));

            communityFcmNotifier.notifyCommentCreated(List.of(RECEIVER_ID), push(CommunityNotificationType.COMMENT));

            then(fcmService).should(never()).send(any());
        }
    }
}
