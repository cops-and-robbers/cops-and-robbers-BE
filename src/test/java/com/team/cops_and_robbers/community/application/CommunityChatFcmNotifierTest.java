package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fcm.FcmMessage;
import com.team.cops_and_robbers.common.fcm.FcmService;
import com.team.cops_and_robbers.common.fixture.UserDeviceFixture;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class CommunityChatFcmNotifierTest extends ServiceUnitTest {

    private static final Long POST_ID = 1L;
    private static final Long SENDER_ID = 100L;
    private static final Long RECEIVER_ID = 200L;

    @InjectMocks
    private CommunityChatFcmNotifier communityChatFcmNotifier;

    @Mock
    private FcmService fcmService;

    private CommunityChatMessage message(CommunityChatMessageType messageType, String body) {
        CommunityChatMessage message = CommunityChatMessage.createMessage(
                "key", POST_ID, SENDER_ID, "보낸사람", User.DEFAULT_PROFILE_ICON, body, messageType);
        setId(message, 10L);
        return message;
    }

    private UserDevice deviceOf(Long userId, String nickname, boolean allowCommunityPush) {
        User user = USER(nickname);
        setId(user, userId);
        user.updateCommunityPush(allowCommunityPush);
        return UserDeviceFixture.IOS_DEVICE(user);
    }

    private void givenPushTargets(Long... userIds) {
        given(communityChatMemberRepository.findPushTargetUserIds(POST_ID, SENDER_ID))
                .willReturn(List.of(userIds));
    }

    private void givenDevice(Long userId, boolean allowCommunityPush) {
        given(userDeviceRepository.findByUser_IdIn(List.of(userId)))
                .willReturn(List.of(deviceOf(userId, "받는사람", allowCommunityPush)));
    }

    private FcmMessage captureSent() {
        ArgumentCaptor<FcmMessage> captor = ArgumentCaptor.forClass(FcmMessage.class);
        then(fcmService).should().send(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("채팅 푸시 발송")
    class NotifyMessageSent {

        @Test
        void 보낸_사람의_닉네임과_메시지로_발송한다() {
            givenPushTargets(RECEIVER_ID);
            givenDevice(RECEIVER_ID, true);

            communityChatFcmNotifier.notifyMessageSent(message(CommunityChatMessageType.TEXT, "다들 오셨나요"));

            FcmMessage sent = captureSent();
            assertSoftly(softly -> {
                softly.assertThat(sent.tokens()).containsExactly("fcm-123");
                softly.assertThat(sent.title()).isEqualTo("보낸사람");
                softly.assertThat(sent.body()).isEqualTo("다들 오셨나요");
                softly.assertThat(sent.data()).containsEntry("type", "TEXT");
                softly.assertThat(sent.data()).containsEntry("postId", "1");
            });
        }

        @Test
        void 게임_초대는_본문_대신_고정_문구로_발송한다() {
            givenPushTargets(RECEIVER_ID);
            givenDevice(RECEIVER_ID, true);

            communityChatFcmNotifier.notifyMessageSent(
                    message(CommunityChatMessageType.GAME_INVITE, "{\"gameId\":1,\"inviteCode\":\"ABC123\"}"));

            assertThat(captureSent().body()).isEqualTo("게임에 초대했습니다");
        }

        @Test
        void 시스템_메시지는_발송하지_않는다() {
            communityChatFcmNotifier.notifyMessageSent(
                    message(CommunityChatMessageType.SYSTEM, "홍길동님이 참여했습니다"));

            then(communityChatMemberRepository).should(never()).findPushTargetUserIds(any(), any());
            then(fcmService).should(never()).send(any());
        }

        @Test
        void 보낸_사람_본인은_대상에서_빠진다() {
            givenPushTargets();

            communityChatFcmNotifier.notifyMessageSent(message(CommunityChatMessageType.TEXT, "다들 오셨나요"));

            then(fcmService).should(never()).send(any());
        }

        @Test
        void 방_알림을_끈_사람에게는_발송하지_않는다() {
            givenPushTargets();

            communityChatFcmNotifier.notifyMessageSent(message(CommunityChatMessageType.TEXT, "다들 오셨나요"));

            then(fcmService).should(never()).send(any());
        }

        @Test
        void 커뮤니티_푸시_전체를_끈_사람에게는_방_설정이_켜져_있어도_발송하지_않는다() {
            givenPushTargets(RECEIVER_ID);
            givenDevice(RECEIVER_ID, false);

            communityChatFcmNotifier.notifyMessageSent(message(CommunityChatMessageType.TEXT, "다들 오셨나요"));

            then(fcmService).should(never()).send(any());
        }

        @Test
        void 등록된_기기가_없으면_발송하지_않는다() {
            givenPushTargets(RECEIVER_ID);
            given(userDeviceRepository.findByUser_IdIn(List.of(RECEIVER_ID))).willReturn(List.of());

            communityChatFcmNotifier.notifyMessageSent(message(CommunityChatMessageType.TEXT, "다들 오셨나요"));

            then(fcmService).should(never()).send(any());
        }

        @Test
        void 발송에_실패해도_예외를_밖으로_던지지_않는다() {
            given(communityChatMemberRepository.findPushTargetUserIds(POST_ID, SENDER_ID))
                    .willThrow(new IllegalStateException("boom"));

            communityChatFcmNotifier.notifyMessageSent(message(CommunityChatMessageType.TEXT, "다들 오셨나요"));

            then(fcmService).should(never()).send(any());
        }
    }
}
