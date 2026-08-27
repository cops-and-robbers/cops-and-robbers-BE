package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatJoinCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatKickCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityChatLeaveCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityChatMemberListResult;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.COMPLETED_POST;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.PAST_POST;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

    private CommunityChatMessage givenSystemMessage(String eventJson) {
        return CommunityChatMessage.createMessage("key-1", POST_ID, JOINER_ID, USER().getNickname(),
                USER().getProfileIcon(), eventJson, CommunityChatMessageType.SYSTEM);
    }

    @Nested
    @DisplayName("채팅방 참여")
    class Join {

        @Test
        void 참여하면_멤버와_JOIN_시스템_메시지가_저장된다() {
            CommunityChatMessage joinMessage = givenSystemMessage("{\"event\":\"JOIN\"}");
            given(communityPostRepository.getByPostIdForUpdate(POST_ID)).willReturn(givenPost());
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, JOINER_ID)).willReturn(false);
            given(communityChatMemberRepository.countByUserId(JOINER_ID)).willReturn(0);
            given(communityChatMemberRepository.countByCommunityPostId(POST_ID)).willReturn(1);
            given(userRepository.getByUserId(JOINER_ID)).willReturn(USER());
            given(communityChatSystemMessageFactory.createJoinMessage(anyLong(), any())).willReturn(joinMessage);

            communityChatMemberService.join(CommunityChatJoinCommand.of(JOINER_ID, POST_ID));

            then(communityChatMemberRepository).should().save(any(CommunityChatMember.class));
            then(communityChatMessageRepository).should().save(joinMessage);
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
        void 모임_날짜가_지난_게시글에는_참여할_수_없다() {
            CommunityPost ended = PAST_POST(AUTHOR_ID);
            setId(ended, POST_ID);
            given(communityPostRepository.getByPostIdForUpdate(POST_ID)).willReturn(ended);

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
            CommunityChatMessage leaveMessage = givenSystemMessage("{\"event\":\"LEAVE\"}");
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(communityChatMemberRepository.findByCommunityPostIdAndUserId(POST_ID, JOINER_ID))
                    .willReturn(Optional.of(member));
            given(userRepository.getByUserId(JOINER_ID)).willReturn(USER());
            given(communityChatSystemMessageFactory.createLeaveMessage(anyLong(), any())).willReturn(leaveMessage);

            communityChatMemberService.leave(CommunityChatLeaveCommand.of(JOINER_ID, POST_ID));

            then(communityChatMessageRepository).should().save(leaveMessage);
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

    @Nested
    @DisplayName("채팅방 멤버 강퇴")
    class Kick {

        @Test
        void 방장이_멤버를_강퇴하면_강퇴_시스템_메시지_저장_후_멤버가_삭제된다() {
            CommunityChatMember target = CommunityChatMember.createMember(POST_ID, JOINER_ID);
            CommunityChatMessage kickMessage = givenSystemMessage("{\"event\":\"KICK\"}");
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(communityChatMemberRepository.findByCommunityPostIdAndUserId(POST_ID, JOINER_ID))
                    .willReturn(Optional.of(target));
            given(userRepository.findById(JOINER_ID)).willReturn(Optional.of(USER("참여자")));
            given(communityChatSystemMessageFactory.createKickMessage(
                    POST_ID, JOINER_ID, "참여자", User.DEFAULT_PROFILE_ICON))
                    .willReturn(kickMessage);

            communityChatMemberService.kick(CommunityChatKickCommand.of(AUTHOR_ID, POST_ID, JOINER_ID));

            then(communityChatMessageRepository).should().save(kickMessage);
            then(communityChatMemberRepository).should().delete(target);
            then(eventPublisher).should().publishEvent(any(Object.class));
        }

        @Test
        void 방장이_아니면_강퇴할_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());

            assertThatThrownBy(() -> communityChatMemberService.kick(
                    CommunityChatKickCommand.of(JOINER_ID, POST_ID, AUTHOR_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.FORBIDDEN_NOT_CHAT_HOST.getDetail());

            then(communityChatMemberRepository).should(never()).delete(any());
        }

        @Test
        void 자기_자신은_강퇴할_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());

            assertThatThrownBy(() -> communityChatMemberService.kick(
                    CommunityChatKickCommand.of(AUTHOR_ID, POST_ID, AUTHOR_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.CANNOT_KICK_SELF.getDetail());

            then(communityChatMemberRepository).should(never()).delete(any());
        }

        @Test
        void 대상이_멤버가_아니면_강퇴할_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(communityChatMemberRepository.findByCommunityPostIdAndUserId(POST_ID, JOINER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> communityChatMemberService.kick(
                    CommunityChatKickCommand.of(AUTHOR_ID, POST_ID, JOINER_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.CHAT_MEMBER_NOT_FOUND.getDetail());
        }

        @Test
        void 탈퇴한_유저도_알수없음_닉네임으로_강퇴할_수_있다() {
            CommunityChatMember target = CommunityChatMember.createMember(POST_ID, JOINER_ID);
            CommunityChatMessage kickMessage = givenSystemMessage("{\"event\":\"KICK\"}");
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(communityChatMemberRepository.findByCommunityPostIdAndUserId(POST_ID, JOINER_ID))
                    .willReturn(Optional.of(target));
            given(userRepository.findById(JOINER_ID)).willReturn(Optional.empty());
            given(communityChatSystemMessageFactory.createKickMessage(
                    POST_ID, JOINER_ID, User.UNKNOWN_NICKNAME, User.DEFAULT_PROFILE_ICON))
                    .willReturn(kickMessage);

            communityChatMemberService.kick(CommunityChatKickCommand.of(AUTHOR_ID, POST_ID, JOINER_ID));

            then(communityChatMemberRepository).should().delete(target);
        }
    }

    @Nested
    @DisplayName("채팅방 멤버 목록 조회")
    class GetMembers {

        @Test
        void 작성자_여부와_닉네임_아이콘이_포함된_멤버_목록을_반환한다() {
            CommunityChatMember author = CommunityChatMember.createMember(POST_ID, AUTHOR_ID);
            CommunityChatMember joiner = CommunityChatMember.createMember(POST_ID, JOINER_ID);
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, AUTHOR_ID)).willReturn(true);
            given(communityChatMemberRepository.findAllByCommunityPostId(POST_ID))
                    .willReturn(List.of(author, joiner));
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(userRepository.findAllById(anyList())).willReturn(List.of(
                    userWithId(AUTHOR_ID, "작성자", 2), userWithId(JOINER_ID, "참여자", 1)));

            CommunityChatMemberListResult result = communityChatMemberService.getMembers(POST_ID, AUTHOR_ID);

            assertThat(result.members()).hasSize(2);
            CommunityChatMemberListResult.Member authorMember = result.members().stream()
                    .filter(member -> member.userId().equals(AUTHOR_ID)).findFirst().orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(authorMember.nickname()).isEqualTo("작성자");
                softly.assertThat(authorMember.profileIcon()).isEqualTo(2);
                softly.assertThat(authorMember.isAuthor()).isTrue();
            });
        }

        @Test
        void 탈퇴한_멤버는_알수없음_닉네임과_기본_아이콘으로_내려온다() {
            CommunityChatMember withdrawn = CommunityChatMember.createMember(POST_ID, JOINER_ID);
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, AUTHOR_ID)).willReturn(true);
            given(communityChatMemberRepository.findAllByCommunityPostId(POST_ID)).willReturn(List.of(withdrawn));
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(givenPost());
            given(userRepository.findAllById(anyList())).willReturn(List.of());

            CommunityChatMemberListResult result = communityChatMemberService.getMembers(POST_ID, AUTHOR_ID);

            CommunityChatMemberListResult.Member member = result.members().getFirst();
            assertSoftly(softly -> {
                softly.assertThat(member.nickname()).isEqualTo("알수없음");
                softly.assertThat(member.profileIcon()).isEqualTo(User.DEFAULT_PROFILE_ICON);
                softly.assertThat(member.isAuthor()).isFalse();
            });
        }

        @Test
        void 멤버가_아니면_조회할_수_없다() {
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(POST_ID, JOINER_ID)).willReturn(false);

            assertThatThrownBy(() -> communityChatMemberService.getMembers(POST_ID, JOINER_ID))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.NOT_A_CHAT_MEMBER.getDetail());

            then(communityChatMemberRepository).should(never()).findAllByCommunityPostId(any());
        }

        private User userWithId(Long id, String nickname, int profileIcon) {
            User user = USER(nickname);
            setId(user, id);
            ReflectionTestUtils.setField(user, "profileIcon", profileIcon);
            return user;
        }
    }
}
