package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostCreateCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostDeleteCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostListCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostStatusCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostUpdateCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class CommunityPostServiceTest extends ServiceUnitTest {

    @InjectMocks
    private CommunityPostService communityPostService;

    @Nested
    @DisplayName("게시글 생성")
    class Create {

        @Test
        void 사용자는_게시글을_생성하고_CommunityPostResult를_반환한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(userRepository.getByUserId(1L)).willReturn(USER());
            given(communityPostRepository.save(any())).willReturn(post);

            CommunityPostResult result = communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "같이 경찰과 도둑 하실 분!", "강남역 근처에서 5명 모집합니다.",
                            LocalDateTime.now().plusDays(3), 37.4979, 127.0276, 6));

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("같이 경찰과 도둑 하실 분!");
            assertThat(result.content()).isEqualTo("강남역 근처에서 5명 모집합니다.");
            assertThat(result.maxParticipants()).isEqualTo(6);
            assertThat(result.status()).isEqualTo(RecruitmentStatus.RECRUITING);
        }

        @Test
        void 게시글을_생성하면_작성자가_채팅방_멤버로_자동_등록된다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(userRepository.getByUserId(1L)).willReturn(USER());
            given(communityPostRepository.save(any())).willReturn(post);

            communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "같이 경찰과 도둑 하실 분!", "강남역 근처에서 5명 모집합니다.",
                            LocalDateTime.now().plusDays(3), 37.4979, 127.0276, 6));

            then(communityChatMemberRepository).should().save(any());
        }

        @Test
        void 과거_모임_날짜로_생성하면_INVALID_MEETING_DATE_예외가_발생한다() {
            given(userRepository.getByUserId(1L)).willReturn(USER());

            assertThatThrownBy(() -> communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "제목", "내용",
                            LocalDateTime.now().minusDays(1), 37.4979, 127.0276, 6)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.INVALID_MEETING_DATE.getDetail());
        }
    }

    @Nested
    @DisplayName("게시글 목록 조회")
    class GetAll {

        @Test
        void 게시글_목록이_최신순으로_반환된다() {
            CommunityPost post1 = POST(1L);
            CommunityPost post2 = POST(2L);
            setId(post1, 1L);
            setId(post2, 2L);
            given(communityPostRepository.findAllByOrderByCreatedAtDesc(any()))
                    .willReturn(new PageImpl<>(List.of(post2, post1)));

            Page<CommunityPostResult> result = communityPostService.getPostList(
                    new CommunityPostListCommand(0, 10));

            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("게시글 단건 조회")
    class GetOne {

        @Test
        void 존재하는_ID로_조회하면_CommunityPostResult를_반환한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);

            CommunityPostResult result = communityPostService.getPost(1L);

            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        void 존재하지_않는_ID로_조회하면_POST_NOT_FOUND_예외가_발생한다() {
            given(communityPostRepository.getByPostId(999L))
                    .willThrow(new ApplicationException(CommunityPostException.POST_NOT_FOUND));

            assertThatThrownBy(() -> communityPostService.getPost(999L))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.POST_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class Update {

        @Test
        void 작성자가_게시글을_수정하고_변경된_값을_반환한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);

            CommunityPostResult result = communityPostService.updatePost(
                    new CommunityPostUpdateCommand(1L, 1L, "수정된 제목", "수정된 내용",
                            LocalDateTime.now().plusDays(5), 37.5665, 126.9780, 8));

            assertThat(result.title()).isEqualTo("수정된 제목");
            assertThat(result.content()).isEqualTo("수정된 내용");
            assertThat(result.maxParticipants()).isEqualTo(8);
        }

        @Test
        void 작성자가_아닌_사용자가_수정하면_FORBIDDEN_NOT_AUTHOR_예외가_발생한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);

            assertThatThrownBy(() -> communityPostService.updatePost(
                    new CommunityPostUpdateCommand(999L, 1L, "수정된 제목", "수정된 내용",
                            LocalDateTime.now().plusDays(5), 37.5665, 126.9780, 8)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.FORBIDDEN_NOT_AUTHOR.getDetail());
        }

        @Test
        void 과거_모임_날짜로_수정하면_INVALID_MEETING_DATE_예외가_발생한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);

            assertThatThrownBy(() -> communityPostService.updatePost(
                    new CommunityPostUpdateCommand(1L, 1L, "수정된 제목", "수정된 내용",
                            LocalDateTime.now().minusDays(1), 37.5665, 126.9780, 8)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.INVALID_MEETING_DATE.getDetail());
        }

        @Test
        void 존재하지_않는_ID로_수정하면_POST_NOT_FOUND_예외가_발생한다() {
            given(communityPostRepository.getByPostId(999L))
                    .willThrow(new ApplicationException(CommunityPostException.POST_NOT_FOUND));

            assertThatThrownBy(() -> communityPostService.updatePost(
                    new CommunityPostUpdateCommand(1L, 999L, "수정된 제목", "수정된 내용",
                            LocalDateTime.now().plusDays(5), 37.5665, 126.9780, 8)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.POST_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class Delete {

        @Test
        void 작성자가_게시글을_삭제하면_deleteByPostId가_호출된다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostIdForUpdate(1L)).willReturn(post);

            communityPostService.deletePost(new CommunityPostDeleteCommand(1L, 1L));

            then(communityPostRepository).should().deleteByPostId(1L);
        }

        @Test
        void 게시글을_삭제하면_채팅_메시지와_멤버도_함께_정리된다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostIdForUpdate(1L)).willReturn(post);

            communityPostService.deletePost(new CommunityPostDeleteCommand(1L, 1L));

            then(communityChatMessageRepository).should().deleteAllByCommunityPostId(1L);
            then(communityChatMemberRepository).should().deleteAllByCommunityPostId(1L);
        }

        @Test
        void 작성자가_아닌_사용자가_삭제하면_FORBIDDEN_NOT_AUTHOR_예외가_발생한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostIdForUpdate(1L)).willReturn(post);

            assertThatThrownBy(() -> communityPostService.deletePost(new CommunityPostDeleteCommand(999L, 1L)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.FORBIDDEN_NOT_AUTHOR.getDetail());
        }

        @Test
        void 존재하지_않는_ID로_삭제하면_POST_NOT_FOUND_예외가_발생한다() {
            given(communityPostRepository.getByPostIdForUpdate(999L))
                    .willThrow(new ApplicationException(CommunityPostException.POST_NOT_FOUND));

            assertThatThrownBy(() -> communityPostService.deletePost(new CommunityPostDeleteCommand(1L, 999L)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.POST_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("모집 상태 변경")
    class UpdateStatus {

        @Test
        void 작성자가_모집_상태를_변경하면_변경된_상태를_반환한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);

            CommunityPostResult result = communityPostService.updateStatus(
                    new CommunityPostStatusCommand(1L, 1L, RecruitmentStatus.COMPLETED));

            assertThat(result.status()).isEqualTo(RecruitmentStatus.COMPLETED);
        }

        @Test
        void 작성자가_아닌_사용자가_상태_변경하면_FORBIDDEN_NOT_AUTHOR_예외가_발생한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);

            assertThatThrownBy(() -> communityPostService.updateStatus(
                    new CommunityPostStatusCommand(999L, 1L, RecruitmentStatus.COMPLETED)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.FORBIDDEN_NOT_AUTHOR.getDetail());
        }

        @Test
        void 존재하지_않는_ID로_상태_변경하면_POST_NOT_FOUND_예외가_발생한다() {
            given(communityPostRepository.getByPostId(999L))
                    .willThrow(new ApplicationException(CommunityPostException.POST_NOT_FOUND));

            assertThatThrownBy(() -> communityPostService.updateStatus(
                    new CommunityPostStatusCommand(1L, 999L, RecruitmentStatus.COMPLETED)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.POST_NOT_FOUND.getDetail());
        }
    }
}
