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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

class GameFcmNotifierTest extends ServiceUnitTest {

    @InjectMocks
    private GameFcmNotifier gameFcmNotifier;

    @Mock
    private FcmService fcmService;

    @Mock
    private InGameParticipantCacheRepository inGameParticipantCacheRepository;

    @Mock
    private ChatPushDebouncer chatPushDebouncer;

    private static final Long TEST_GAME_ID = 1L;
    private static final Long SENDER_PARTICIPANT_ID = 10L;
    private static final Long POLICE_PARTICIPANT_ID = 11L;
    private static final Long ROBBER_PARTICIPANT_ID = 12L;

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

        @BeforeEach
        void setUp() {
            // 창이 닫힌 케이스에선 이 스텁들이 호출되지 않으므로 lenient
            lenient().when(chatPushDebouncer.tryOpenWindow(eq(TEST_GAME_ID), anyString())).thenReturn(true);
            // 발신자(경찰) + 경찰 1명 + 도둑 1명, 전원 오프라인
            lenient().when(inGameParticipantCacheRepository.findAllEntriesByGameId(TEST_GAME_ID)).thenReturn(Map.of(
                    SENDER_PARTICIPANT_ID, new InGameParticipantCache("보낸사람", Team.POLICE, "sender-token"),
                    POLICE_PARTICIPANT_ID, new InGameParticipantCache("경찰", Team.POLICE, "police-token"),
                    ROBBER_PARTICIPANT_ID, new InGameParticipantCache("도둑", Team.ROBBER, "robber-token")
            ));
        }

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
            // when
            gameFcmNotifier.notifyChatMessage(chatMessage(ChatScope.ALL));

            // then
            assertSoftly(softly -> {
                FcmMessage sent = captureSent();
                softly.assertThat(sent.tokens()).containsExactlyInAnyOrder("police-token", "robber-token");
                // 방 단위 발송이라 특정 메시지 내용이 아니라 고정 문구를 쓴다
                softly.assertThat(sent.title()).isEqualTo("새 채팅");
                softly.assertThat(sent.body()).isEqualTo("새 채팅이 도착했습니다.");
                softly.assertThat(sent.data()).containsEntry("type", "CHAT");
                softly.assertThat(sent.data()).containsEntry("gameId", "1");
                softly.assertThat(sent.data()).containsEntry("scope", "ALL");
                softly.assertThat(sent.collapseKey()).isEqualTo("chat:1");
            });
        }

        @Test
        void 팀_채팅은_발신자와_같은_팀에게만_발송한다() {
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
        void 창이_열려_있으면_대상이_있어도_발송하지_않는다() {
            // given
            given(chatPushDebouncer.tryOpenWindow(eq(TEST_GAME_ID), anyString())).willReturn(false);

            // when
            gameFcmNotifier.notifyChatMessage(chatMessage(ChatScope.ALL));

            // then
            then(fcmService).should(never()).send(any(FcmMessage.class));
        }

        @Test
        void 팀_채팅은_팀별로_분리된_스로틀_창을_사용한다() {
            // when
            gameFcmNotifier.notifyChatMessage(chatMessage(ChatScope.TEAM));

            // then
            ArgumentCaptor<String> scope = ArgumentCaptor.forClass(String.class);
            then(chatPushDebouncer).should().tryOpenWindow(eq(TEST_GAME_ID), scope.capture());
            assertThat(scope.getValue()).isEqualTo("TEAM:POLICE");
        }

        @Test
        void 토큰이_없는_참가자는_수신자에서_제외된다() {
            // given
            given(inGameParticipantCacheRepository.findAllEntriesByGameId(TEST_GAME_ID)).willReturn(Map.of(
                    SENDER_PARTICIPANT_ID, new InGameParticipantCache("보낸사람", Team.POLICE, "sender-token"),
                    POLICE_PARTICIPANT_ID, new InGameParticipantCache("경찰", Team.POLICE, "police-token"),
                    ROBBER_PARTICIPANT_ID, new InGameParticipantCache("도둑", Team.ROBBER, null)
            ));

            // when
            gameFcmNotifier.notifyChatMessage(chatMessage(ChatScope.ALL));

            // then
            FcmMessage sent = captureSent();
            assertSoftly(softly -> softly.assertThat(sent.tokens()).containsExactly("police-token"));
        }

        @Test
        void 발송할_토큰이_없으면_FCM을_발송하지_않는다() {
            // given
            given(inGameParticipantCacheRepository.findAllEntriesByGameId(TEST_GAME_ID)).willReturn(Map.of());

            // when
            gameFcmNotifier.notifyChatMessage(chatMessage(ChatScope.ALL));

            // then
            then(fcmService).should(never()).send(any(FcmMessage.class));
        }

        @Test
        void 발송에_실패해도_예외를_밖으로_던지지_않는다() {
            // given
            given(inGameParticipantCacheRepository.findAllEntriesByGameId(TEST_GAME_ID))
                    .willThrow(new IllegalStateException("boom"));

            // when
            gameFcmNotifier.notifyChatMessage(chatMessage(ChatScope.ALL));

            // then
            then(fcmService).should(never()).send(any(FcmMessage.class));
        }
    }
}
