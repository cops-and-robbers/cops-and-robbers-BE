package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatJoinCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatLeaveCommand;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.COMPLETED_POST;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class CommunityChatMemberServiceTest extends ServiceUnitTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long JOINER_ID = 2L;
    private static final Long POST_ID = 1L;

    @InjectMocks
    private CommunityChatMemberService communityChatMemberService;

    @Mock
    private CommunityChatSystemMessageFactory communityChatSystemMessageFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CommunityPost givenPost() {
        CommunityPost post = POST(AUTHOR_ID);
        setId(post, POST_ID);
        return post;
    }

    @Nested
    @DisplayName("채팅방 참여")
    class Join {

        @Test
        void 참여하면_멤버와_JOIN_시스템_메시지가_저장된다() {
            given(communityPostRepository.getByPostIdForUpdate(POST_ID)).willReturn(givenPost());
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, JOINER_ID)).willReturn(false);
            given(communityChatMemberRepository.countByUserId(JOINER_ID)).willReturn(0);
            given(communityChatMemberRepository.countByCommunityPostId(POST_ID)).willReturn(1);
            given(userRepository.getByUserId(JOINER_ID)).willReturn(USER());

            communityChatMemberService.join(CommunityChatJoinCommand.of(JOINER_ID, POST_ID));

            then(communityChatMemberRepository).should().save(any(CommunityChatMember.class));
            then(communityChatSystemMessageFactory).should().createJoinMessage(anyLong(), any());
            then(eventPublisher).should().publishEvent(any(Object.class));
        }

        @Test
        void 모집이_종료된_게시글에는_참여할_수_없다() {
            CommunityPost completed = COMPLETED_POST(AUTHOR_ID);
            setId(completed, POST_ID);
            given(communityPostRepository.getByPostIdForUpdate(POST_ID)).willReturn(completed);

            assertThatThrownBy(() -> communityChatMemberService.join(CommunityChatJoinCommand.of(JOINER_ID, POST_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.RECRUITMENT_CLOSED.getDetail());

            then(communityChatMemberRepository).should(never()).save(any());
        }

        @Test
        void 이미_참여한_채팅방에는_다시_참여할_수_없다() {
            given(communityPostRepository.getByPostIdForUpdate(POST_ID)).willReturn(givenPost());
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, JOINER_ID)).willReturn(true);

            assertThatThrownBy(() -> communityChatMemberService.join(CommunityChatJoinCommand.of(JOINER_ID, POST_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.ALREADY_JOINED.getDetail());
        }

        @Test
        void 참여_채팅방이_상한에_도달하면_참여할_수_없다() {
            given(communityPostRepository.getByPostIdForUpdate(POST_ID)).willReturn(givenPost());
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, JOINER_ID)).willReturn(false);
            given(communityChatMemberRepository.countByUserId(JOINER_ID)).willReturn(100);

            assertThatThrownBy(() -> communityChatMemberService.join(CommunityChatJoinCommand.of(JOINER_ID, POST_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.JOINED_CHAT_ROOM_LIMIT_EXCEEDED.getDetail());

            then(communityChatMemberRepository).should(never()).save(any());
        }

        @Test
        void 모집_정원이_가득_차면_참여할_수_없다() {
            CommunityPost post = givenPost();
            given(communityPostRepository.getByPostIdForUpdate(POST_ID)).willReturn(post);
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, JOINER_ID)).willReturn(false);
            given(communityChatMemberRepository.countByUserId(JOINER_ID)).willReturn(0);
            given(communityChatMemberRepository.countByCommunityPostId(POST_ID))
                    .willReturn(post.getMaxParticipants());

            assertThatThrownBy(() -> communityChatMemberService.join(CommunityChatJoinCommand.of(JOINER_ID, POST_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.CHAT_ROOM_FULL.getDetail());

            then(communityChatMemberRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("채팅방 나가기")
    class Leave {

        @Test
        void 나가면_LEAVE_시스템_메시지_저장_후_멤버가_삭제된다() {
            CommunityChatMember member = CommunityChatMember.createMember(POST_ID, JOINER_ID);
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(communityChatMemberRepository.findByCommunityPostIdAndUserId(POST_ID, JOINER_ID))
                    .willReturn(Optional.of(member));
            given(userRepository.getByUserId(JOINER_ID)).willReturn(USER());

            communityChatMemberService.leave(CommunityChatLeaveCommand.of(JOINER_ID, POST_ID));

            then(communityChatSystemMessageFactory).should().createLeaveMessage(anyLong(), any());
            then(communityChatMemberRepository).should().delete(member);
            then(eventPublisher).should().publishEvent(any(Object.class));
        }

        @Test
        void 작성자는_자신의_채팅방을_나갈_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());

            assertThatThrownBy(() -> communityChatMemberService.leave(CommunityChatLeaveCommand.of(AUTHOR_ID, POST_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.AUTHOR_CANNOT_LEAVE.getDetail());

            then(communityChatMemberRepository).should(never()).delete(any());
        }

        @Test
        void 멤버가_아니면_나갈_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(communityChatMemberRepository.findByCommunityPostIdAndUserId(POST_ID, JOINER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> communityChatMemberService.leave(CommunityChatLeaveCommand.of(JOINER_ID, POST_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.NOT_A_CHAT_MEMBER.getDetail());
        }
    }
}
