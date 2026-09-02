package com.team.cops_and_robbers.community.chat.pin.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.chat.common.application.CommunityChatSystemMessageFactory;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.chat.common.exception.CommunityChatException;
import com.team.cops_and_robbers.community.chat.pin.application.dto.command.CommunityChatPinDeleteCommand;
import com.team.cops_and_robbers.community.chat.pin.application.dto.command.CommunityChatPinRegisterCommand;
import com.team.cops_and_robbers.community.chat.pin.application.dto.command.CommunityChatPinUpdateCommand;
import com.team.cops_and_robbers.community.chat.pin.application.dto.result.CommunityChatPinResult;
import com.team.cops_and_robbers.community.chat.pin.domain.CommunityChatPin;
import com.team.cops_and_robbers.community.post.domain.CommunityPost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class CommunityChatPinServiceTest extends ServiceUnitTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long POST_ID = 1L;

    @InjectMocks
    private CommunityChatPinService communityChatPinService;

    @Mock
    private CommunityChatSystemMessageFactory communityChatSystemMessageFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CommunityPost givenPost() {
        CommunityPost post = POST(AUTHOR_ID);
        setId(post, POST_ID);
        return post;
    }

    private CommunityChatMessage givenSystemMessage() {
        return CommunityChatMessage.createMessage("key-1", POST_ID, AUTHOR_ID, USER().getNickname(),
                USER().getProfileIcon(), "{\"event\":\"PIN_REGISTERED\"}", CommunityChatMessageType.SYSTEM);
    }

    private void stampTimestamps(CommunityChatPin pin) {
        ReflectionTestUtils.setField(pin, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(pin, "updatedAt", LocalDateTime.now());
    }

    private void givenSaveEchoesArgument() {
        given(communityChatPinRepository.save(any(CommunityChatPin.class)))
                .willAnswer(invocation -> {
                    CommunityChatPin saved = invocation.getArgument(0);
                    stampTimestamps(saved);
                    return saved;
                });
    }

    private void givenRegisteredMessage() {
        given(communityChatSystemMessageFactory.createPinRegisteredMessage(anyLong(), any()))
                .willReturn(givenSystemMessage());
    }

    private void givenUpdatedMessage() {
        given(communityChatSystemMessageFactory.createPinUpdatedMessage(anyLong(), any()))
                .willReturn(givenSystemMessage());
    }

    private void givenDeletedMessage() {
        given(communityChatSystemMessageFactory.createPinDeletedMessage(anyLong(), any()))
                .willReturn(givenSystemMessage());
    }

    @Nested
    @DisplayName("고정 채팅 등록")
    class Register {

        @Test
        void 방장이_등록하면_고정_채팅과_시스템_메시지가_저장된다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(userRepository.getByUserId(AUTHOR_ID)).willReturn(USER());
            givenSaveEchoesArgument();
            givenRegisteredMessage();

            CommunityChatPinResult result = communityChatPinService.register(
                    CommunityChatPinRegisterCommand.of(AUTHOR_ID, POST_ID, "정문에서 만나요"));

            assertSoftly(softly -> {
                softly.assertThat(result.content()).isEqualTo("정문에서 만나요");
                softly.assertThat(result.postId()).isEqualTo(POST_ID);
            });
            then(communityChatPinRepository).should().deleteByCommunityPostId(POST_ID);
            then(communityChatPinRepository).should().save(any(CommunityChatPin.class));
            then(communityChatMessageRepository).should().save(any(CommunityChatMessage.class));
            then(eventPublisher).should(times(2)).publishEvent(any(Object.class));
        }

        @Test
        void 이미_등록된_것이_있으면_삭제_후_새로_등록한다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(userRepository.getByUserId(AUTHOR_ID)).willReturn(USER());
            givenSaveEchoesArgument();
            givenRegisteredMessage();

            communityChatPinService.register(CommunityChatPinRegisterCommand.of(AUTHOR_ID, POST_ID, "새 공지"));

            then(communityChatPinRepository).should().deleteByCommunityPostId(POST_ID);
            then(communityChatPinRepository).should().save(any(CommunityChatPin.class));
        }

        @Test
        void 방장이_아니면_등록할_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());

            assertThatThrownBy(() -> communityChatPinService.register(
                    CommunityChatPinRegisterCommand.of(OTHER_ID, POST_ID, "정문에서 만나요")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.FORBIDDEN_NOT_CHAT_PIN_HOST.getDetail());

            then(communityChatPinRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("고정 채팅 수정")
    class Update {

        @Test
        void 방장이_수정하면_내용이_바뀌고_시스템_메시지가_저장된다() {
            CommunityChatPin pin = CommunityChatPin.createPin(POST_ID, AUTHOR_ID, "기존 내용");
            stampTimestamps(pin);
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(communityChatPinRepository.findByCommunityPostId(POST_ID)).willReturn(Optional.of(pin));
            given(userRepository.getByUserId(AUTHOR_ID)).willReturn(USER());
            givenUpdatedMessage();

            CommunityChatPinResult result = communityChatPinService.update(
                    CommunityChatPinUpdateCommand.of(AUTHOR_ID, POST_ID, "바뀐 내용"));

            assertThat(result.content()).isEqualTo("바뀐 내용");
            then(communityChatMessageRepository).should().save(any(CommunityChatMessage.class));
            then(eventPublisher).should(times(2)).publishEvent(any(Object.class));
        }

        @Test
        void 방장이_아니면_수정할_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());

            assertThatThrownBy(() -> communityChatPinService.update(
                    CommunityChatPinUpdateCommand.of(OTHER_ID, POST_ID, "바뀐 내용")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.FORBIDDEN_NOT_CHAT_PIN_HOST.getDetail());
        }

        @Test
        void 등록된_것이_없으면_수정할_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(communityChatPinRepository.findByCommunityPostId(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> communityChatPinService.update(
                    CommunityChatPinUpdateCommand.of(AUTHOR_ID, POST_ID, "바뀐 내용")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.CHAT_PIN_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("고정 채팅 삭제")
    class Delete {

        @Test
        void 방장이_삭제하면_고정_채팅이_지워지고_시스템_메시지가_저장된다() {
            CommunityChatPin pin = CommunityChatPin.createPin(POST_ID, AUTHOR_ID, "기존 내용");
            stampTimestamps(pin);
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(communityChatPinRepository.findByCommunityPostId(POST_ID)).willReturn(Optional.of(pin));
            given(userRepository.getByUserId(AUTHOR_ID)).willReturn(USER());
            givenDeletedMessage();

            communityChatPinService.delete(CommunityChatPinDeleteCommand.of(AUTHOR_ID, POST_ID));

            then(communityChatPinRepository).should().delete(pin);
            then(communityChatMessageRepository).should().save(any(CommunityChatMessage.class));
            then(eventPublisher).should(times(2)).publishEvent(any(Object.class));
        }

        @Test
        void 방장이_아니면_삭제할_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());

            assertThatThrownBy(() -> communityChatPinService.delete(
                    CommunityChatPinDeleteCommand.of(OTHER_ID, POST_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.FORBIDDEN_NOT_CHAT_PIN_HOST.getDetail());

            then(communityChatPinRepository).should(never()).delete(any());
        }

        @Test
        void 등록된_것이_없으면_삭제할_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(communityChatPinRepository.findByCommunityPostId(POST_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> communityChatPinService.delete(
                    CommunityChatPinDeleteCommand.of(AUTHOR_ID, POST_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.CHAT_PIN_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("고정 채팅 조회")
    class Get {

        @Test
        void 등록된_고정_채팅을_반환한다() {
            CommunityChatPin pin = CommunityChatPin.createPin(POST_ID, AUTHOR_ID, "정문에서 만나요");
            stampTimestamps(pin);
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, AUTHOR_ID)).willReturn(true);
            given(communityChatPinRepository.findByCommunityPostId(POST_ID)).willReturn(Optional.of(pin));
            given(userRepository.findById(AUTHOR_ID)).willReturn(Optional.of(USER()));

            CommunityChatPinResult result = communityChatPinService.get(POST_ID, AUTHOR_ID);

            assertThat(result.content()).isEqualTo("정문에서 만나요");
        }

        @Test
        void 등록된_것이_없으면_예외_없이_빈_결과를_반환한다() {
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, AUTHOR_ID)).willReturn(true);
            given(communityChatPinRepository.findByCommunityPostId(POST_ID)).willReturn(Optional.empty());

            CommunityChatPinResult result = communityChatPinService.get(POST_ID, AUTHOR_ID);

            assertSoftly(softly -> {
                softly.assertThat(result.postId()).isEqualTo(POST_ID);
                softly.assertThat(result.content()).isNull();
                softly.assertThat(result.id()).isNull();
            });
        }

        @Test
        void 멤버가_아니면_조회할_수_없다() {
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, OTHER_ID)).willReturn(false);

            assertThatThrownBy(() -> communityChatPinService.get(POST_ID, OTHER_ID))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.NOT_A_CHAT_MEMBER.getDetail());
        }
    }
}
