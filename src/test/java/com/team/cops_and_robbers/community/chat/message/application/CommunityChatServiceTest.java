package com.team.cops_and_robbers.community.chat.message.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatGameInviteData;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.chat.common.exception.CommunityChatException;
import com.team.cops_and_robbers.community.chat.common.repository.CommunityChatSenderProfileProjection;
import com.team.cops_and_robbers.community.chat.message.application.dto.command.CommunityChatSendCommand;
import com.team.cops_and_robbers.user.exception.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class CommunityChatServiceTest extends ServiceUnitTest {

    private static final Long POST_ID = 1L;
    private static final Long SENDER_ID = 2L;
    private static final String NICKNAME = "홍길동";
    private static final int PROFILE_ICON = 2;

    @InjectMocks
    private CommunityChatService communityChatService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private void givenChatMember() {
        givenChatMember(true);
    }

    private void givenChatMember(boolean requiredTermsAgreed) {
        given(communityChatMemberRepository.findSenderProfileByPostIdAndUserId(POST_ID, SENDER_ID))
                .willReturn(Optional.of(
                        new CommunityChatSenderProfileProjection(NICKNAME, PROFILE_ICON, requiredTermsAgreed)));
    }

    private CommunityChatSendCommand textCommand(String messageKey, String message) {
        return CommunityChatSendCommand.of(POST_ID, SENDER_ID, messageKey, message, null,
                CommunityChatMessageType.TEXT);
    }

    private CommunityChatMessage captureSavedMessage() {
        ArgumentCaptor<CommunityChatMessage> captor = ArgumentCaptor.forClass(CommunityChatMessage.class);
        then(communityChatMessageRepository).should().save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("메시지 전송")
    class Send {

        @Test
        void 필수_약관에_동의하지_않았으면_메시지를_보낼_수_없다() {
            givenChatMember(false);

            assertThatThrownBy(() -> communityChatService.send(textCommand("key-1", "안녕하세요")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(UserException.REQUIRED_TERMS_NOT_AGREED.getDetail());

            then(communityChatMessageRepository).should(never()).save(any());
        }

        @Test
        void 멤버가_보낸_메시지는_발신_시점_닉네임과_함께_저장된다() {
            givenChatMember();

            communityChatService.send(textCommand("key-1", "안녕하세요"));

            CommunityChatMessage saved = captureSavedMessage();
            assertThat(saved.getMessage()).isEqualTo("안녕하세요");
            assertThat(saved.getSenderNickname()).isEqualTo(NICKNAME);
            assertThat(saved.getSenderProfileIcon()).isEqualTo(PROFILE_ICON);
            assertThat(saved.getMessageKey()).isEqualTo("key-1");
            then(eventPublisher).should().publishEvent(any(Object.class));
        }

        @Test
        void 멤버가_아니면_전송할_수_없다() {
            given(communityChatMemberRepository.findSenderProfileByPostIdAndUserId(POST_ID, SENDER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> communityChatService.send(textCommand("key-1", "안녕하세요")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.NOT_A_CHAT_MEMBER.getDetail());

            then(communityChatMessageRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("메시지 타입 검증")
    class MessageTypeValidation {

        @Test
        void 클라이언트는_SYSTEM_타입을_보낼_수_없다() {
            CommunityChatSendCommand command = CommunityChatSendCommand.of(
                    POST_ID, SENDER_ID, "key-1", "{\"event\":\"JOIN\"}", null,
                    CommunityChatMessageType.SYSTEM);

            assertThatThrownBy(() -> communityChatService.send(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.INVALID_MESSAGE_TYPE.getDetail());

            then(communityChatMessageRepository).should(never()).save(any());
        }

        @Test
        void 타입이_없으면_전송할_수_없다() {
            CommunityChatSendCommand command =
                    CommunityChatSendCommand.of(POST_ID, SENDER_ID, "key-1", "안녕하세요", null, null);

            assertThatThrownBy(() -> communityChatService.send(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.INVALID_MESSAGE_TYPE.getDetail());
        }
    }

    @Nested
    @DisplayName("본문 검증")
    class MessageValidation {

        @Test
        void 공백만_있는_메시지는_전송할_수_없다() {
            assertThatThrownBy(() -> communityChatService.send(textCommand("key-1", "   ")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.EMPTY_MESSAGE.getDetail());
        }

        @Test
        void 컬럼_길이를_넘는_메시지는_전송할_수_없다() {
            assertThatThrownBy(() -> communityChatService.send(textCommand("key-1", "가".repeat(501))))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.MESSAGE_TOO_LONG.getDetail());
        }

        @Test
        void 검증은_멤버십_조회보다_먼저_수행된다() {
            assertThatThrownBy(() -> communityChatService.send(textCommand("key-1", "   ")))
                    .isInstanceOf(ApplicationException.class);

            then(communityChatMemberRepository).should(never())
                    .findSenderProfileByPostIdAndUserId(any(), any());
        }
    }

    @Nested
    @DisplayName("메시지 키 처리")
    class MessageKey {

        @Test
        void 키를_보내지_않으면_서버가_생성한다() {
            givenChatMember();

            communityChatService.send(textCommand(null, "안녕하세요"));

            assertThat(captureSavedMessage().getMessageKey()).isNotBlank();
        }

        @Test
        void 컬럼_길이를_넘는_키는_전송할_수_없다() {
            assertThatThrownBy(() -> communityChatService.send(textCommand("k".repeat(37), "안녕하세요")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.INVALID_MESSAGE_KEY.getDetail());
        }
    }

    @Nested
    @DisplayName("게임 초대")
    class GameInvite {

        private CommunityChatSendCommand inviteCommand(CommunityChatGameInviteData invite) {
            return CommunityChatSendCommand.of(POST_ID, SENDER_ID, "key-1", null, invite,
                    CommunityChatMessageType.GAME_INVITE);
        }

        @Test
        void 초대_정보는_서버가_JSON으로_직렬화해_저장한다() {
            givenChatMember();

            communityChatService.send(inviteCommand(new CommunityChatGameInviteData("ABC123")));

            CommunityChatMessage saved = captureSavedMessage();
            assertThat(saved.getMessage()).contains("\"inviteCode\":\"ABC123\"");
            assertThat(saved.getSenderNickname()).isEqualTo(NICKNAME);
        }

        @Test
        void 초대_본문에는_닉네임이_저장되지_않아_위장할_수_없다() {
            givenChatMember();

            communityChatService.send(inviteCommand(new CommunityChatGameInviteData("ABC123")));

            assertThat(captureSavedMessage().getMessage()).doesNotContain("inviterNickname");
        }

        @Test
        void 초대_정보가_없으면_전송할_수_없다() {
            assertThatThrownBy(() -> communityChatService.send(inviteCommand(null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.INVALID_GAME_INVITE.getDetail());
        }

        @Test
        void 초대_코드가_비어있으면_전송할_수_없다() {
            assertThatThrownBy(() ->
                    communityChatService.send(inviteCommand(new CommunityChatGameInviteData("  "))))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.INVALID_GAME_INVITE.getDetail());
        }
    }
}
