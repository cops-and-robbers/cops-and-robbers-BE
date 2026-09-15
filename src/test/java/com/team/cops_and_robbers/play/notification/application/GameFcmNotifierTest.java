package com.team.cops_and_robbers.play.notification.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fcm.FcmMessage;
import com.team.cops_and_robbers.common.fcm.FcmService;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.chat.domain.ChatMessage;
import com.team.cops_and_robbers.play.chat.domain.ChatScope;
import com.team.cops_and_robbers.play.chat.domain.ChatSender;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class GameFcmNotifierTest extends ServiceUnitTest {

    @InjectMocks
    private GameFcmNotifier gameFcmNotifier;

    @Mock
    private FcmService fcmService;

    @Mock
    private InGameParticipantCacheRepository inGameParticipantCacheRepository;

    private static final Long TEST_GAME_ID = 1L;
    private static final Long SENDER_PARTICIPANT_ID = 10L;

    @Nested
    @DisplayName("시스템 이벤트 알림 발송")
    class NotifySystemEvent {

        @Test
        void 캐시에_토큰이_있으면_DB_조회_없이_바로_알림을_발송한다() {
            // given
            SystemEvent event = SystemEvent.of(TEST_GAME_ID, SystemEventType.ARREST, null);
            List<InGameParticipantCache> caches = List.of(
                    new InGameParticipantCache("nick1", Team.POLICE, "token1"),
                    new InGameParticipantCache("nick2", Team.ROBBER, "token2")
            );
            given(inGameParticipantCacheRepository.findAllByGameId(TEST_GAME_ID)).willReturn(caches);

            // when
            gameFcmNotifier.notifySystemEvent(event);

            // then
            then(fcmService).should().send(any(FcmMessage.class));
        }

        @Test
        void 모든_유저가_알림_거부_상태이면_FCM을_발송하지_않는다() {
            // given
            SystemEvent event = SystemEvent.of(TEST_GAME_ID, SystemEventType.ARREST, null);
            List<InGameParticipantCache> caches = List.of(
                    new InGameParticipantCache("nick1", Team.POLICE, null),
                    new InGameParticipantCache("nick2", Team.ROBBER, null)
            );
            given(inGameParticipantCacheRepository.findAllByGameId(TEST_GAME_ID)).willReturn(caches);

            // when
            gameFcmNotifier.notifySystemEvent(event);

            // then
            then(fcmService).should(never()).send(any(FcmMessage.class));
        }
    }

    @Nested
    @DisplayName("인게임 채팅 알림 발송")
    class NotifyChatMessage {

        private ChatMessage chatMessage(ChatScope scope) {
            ChatSender sender = ChatSender.of(SENDER_PARTICIPANT_ID, "보낸사람", Team.POLICE);
            return ChatMessage.of(TEST_GAME_ID, sender, "다들 어디야", scope);
        }

        private FcmMessage captureSent() {
            ArgumentCaptor<FcmMessage> captor = ArgumentCaptor.forClass(FcmMessage.class);
            then(fcmService).should().send(captor.capture());
            return captor.getValue();
        }

        @Test
        void 전체_채팅은_팀_구분_없이_발신자를_제외한_전원에게_발송한다() {
            // given
            given(gameParticipantRepository.findChatPushTokens(TEST_GAME_ID, SENDER_PARTICIPANT_ID, null))
                    .willReturn(List.of("token1", "token2"));

            // when
            gameFcmNotifier.notifyChatMessage(chatMessage(ChatScope.ALL));

            // then
            assertSoftly(softly -> {
                FcmMessage sent = captureSent();
                softly.assertThat(sent.tokens()).containsExactly("token1", "token2");
                softly.assertThat(sent.title()).isEqualTo("보낸사람");
                softly.assertThat(sent.body()).isEqualTo("다들 어디야");
                softly.assertThat(sent.data()).containsEntry("type", "CHAT");
                softly.assertThat(sent.data()).containsEntry("gameId", "1");
                softly.assertThat(sent.data()).containsEntry("scope", "ALL");
            });
        }

        @Test
        void 팀_채팅은_발신자와_같은_팀에게만_발송한다() {
            // given
            given(gameParticipantRepository.findChatPushTokens(TEST_GAME_ID, SENDER_PARTICIPANT_ID, Team.POLICE))
                    .willReturn(List.of("police-token"));

            // when
            gameFcmNotifier.notifyChatMessage(chatMessage(ChatScope.TEAM));

            // then
            assertSoftly(softly -> {
                FcmMessage sent = captureSent();
                softly.assertThat(sent.tokens()).containsExactly("police-token");
                softly.assertThat(sent.data()).containsEntry("scope", "TEAM");
            });
        }

        @Test
        void 발송할_토큰이_없으면_FCM을_발송하지_않는다() {
            // given
            given(gameParticipantRepository.findChatPushTokens(TEST_GAME_ID, SENDER_PARTICIPANT_ID, null))
                    .willReturn(List.of());

            // when
            gameFcmNotifier.notifyChatMessage(chatMessage(ChatScope.ALL));

            // then
            then(fcmService).should(never()).send(any(FcmMessage.class));
        }

        @Test
        void 발송에_실패해도_예외를_밖으로_던지지_않는다() {
            // given
            given(gameParticipantRepository.findChatPushTokens(TEST_GAME_ID, SENDER_PARTICIPANT_ID, null))
                    .willThrow(new IllegalStateException("boom"));

            // when
            gameFcmNotifier.notifyChatMessage(chatMessage(ChatScope.ALL));

            // then
            then(fcmService).should(never()).send(any(FcmMessage.class));
        }
    }
}
