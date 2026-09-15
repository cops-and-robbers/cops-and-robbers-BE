package com.team.cops_and_robbers.play.chat.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.chat.application.dto.ChatCommand;
import com.team.cops_and_robbers.play.chat.domain.ChatMessage;
import com.team.cops_and_robbers.play.chat.domain.ChatScope;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.notification.application.GameFcmNotifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class ChatPublisherTest extends ServiceUnitTest {

    private static final Long TEST_GAME_ID = 1L;
    private static final Long SENDER_PARTICIPANT_ID = 10L;
    private static final Long SENDER_USER_ID = 100L;

    @InjectMocks
    private ChatPublisher chatPublisher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private InGameParticipantCacheRepository inGameParticipantCacheRepository;

    @Mock
    private GameFcmNotifier gameFcmNotifier;

    private ChatCommand command(ChatScope scope) {
        return new ChatCommand(TEST_GAME_ID, SENDER_USER_ID, SENDER_PARTICIPANT_ID, "다들 어디야", scope);
    }

    private void givenSenderInCache(Team team) {
        given(inGameParticipantCacheRepository.findByParticipantId(TEST_GAME_ID, SENDER_PARTICIPANT_ID))
                .willReturn(Optional.of(new InGameParticipantCache("보낸사람", team, "token")));
    }

    @Nested
    @DisplayName("채팅 메시지 라우팅")
    class ProcessAndRouteMessage {

        @Test
        void 채팅을_발행하면_FCM_알림도_함께_보낸다() {
            // given
            givenSenderInCache(Team.POLICE);

            // when
            chatPublisher.processAndRouteMessage(command(ChatScope.ALL));

            // then
            ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
            then(gameFcmNotifier).should().notifyChatMessage(captor.capture());

            ChatMessage notified = captor.getValue();
            assertSoftly(softly -> {
                softly.assertThat(notified.gameId()).isEqualTo(TEST_GAME_ID);
                softly.assertThat(notified.sender().participantId()).isEqualTo(SENDER_PARTICIPANT_ID);
                softly.assertThat(notified.sender().nickname()).isEqualTo("보낸사람");
                softly.assertThat(notified.sender().team()).isEqualTo(Team.POLICE);
                softly.assertThat(notified.message()).isEqualTo("다들 어디야");
                softly.assertThat(notified.scope()).isEqualTo(ChatScope.ALL);
            });
        }

        @Test
        void 팀_채팅도_같은_메시지로_FCM_알림을_보낸다() {
            // given
            givenSenderInCache(Team.ROBBER);

            // when
            chatPublisher.processAndRouteMessage(command(ChatScope.TEAM));

            // then
            ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
            then(gameFcmNotifier).should().notifyChatMessage(captor.capture());
            assertThat(captor.getValue().scope()).isEqualTo(ChatScope.TEAM);
        }

        @Test
        void 캐시에_없는_참가자는_예외가_나고_FCM도_보내지_않는다() {
            // given
            given(inGameParticipantCacheRepository.findByParticipantId(TEST_GAME_ID, SENDER_PARTICIPANT_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatPublisher.processAndRouteMessage(command(ChatScope.ALL)))
                    .isInstanceOf(ApplicationException.class);
            then(gameFcmNotifier).should(never()).notifyChatMessage(any());
        }
    }
}
