package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostScrapListCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostScrapListResult;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.CommunityPostScrap;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.exception.CommunityPostReactionException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class CommunityPostReactionServiceTest extends ServiceUnitTest {

    private static final Long POST_ID = 1L;
    private static final Long USER_ID = 100L;

    @InjectMocks
    private CommunityPostReactionService communityPostReactionService;

    private void givenPost() {
        given(communityPostRepository.getByPostId(POST_ID)).willReturn(POST(999L));
    }

    private CommunityPost post(Long id, Long writerId) {
        CommunityPost post = POST(writerId);
        setId(post, id);
        return post;
    }

    private CommunityPostScrap scrap(Long id, Long postId) {
        CommunityPostScrap scrap = CommunityPostScrap.createScrap(postId, USER_ID);
        setId(scrap, id);
        return scrap;
    }

    private User userWithNickname(Long userId, String nickname) {
        User user = USER(nickname);
        setId(user, userId);
        return user;
    }

    @Nested
    @DisplayName("좋아요")
    class Like {

        @Test
        void 게시글에_좋아요를_누르면_저장된다() {
            givenPost();
            given(communityPostLikeRepository.existsByCommunityPostIdAndUserId(POST_ID, USER_ID)).willReturn(false);

            communityPostReactionService.likePost(POST_ID, USER_ID);

            then(communityPostLikeRepository).should().save(any());
        }

        @Test
        void 이미_좋아요한_게시글은_다시_누를_수_없다() {
            givenPost();
            given(communityPostLikeRepository.existsByCommunityPostIdAndUserId(POST_ID, USER_ID)).willReturn(true);

            assertThatThrownBy(() -> communityPostReactionService.likePost(POST_ID, USER_ID))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostReactionException.ALREADY_LIKED.getDetail());

            then(communityPostLikeRepository).should(never()).save(any());
        }

        @Test
        void 동시에_두_번_눌려_유니크_제약이_걸려도_ALREADY_LIKED로_응답한다() {
            givenPost();
            given(communityPostLikeRepository.existsByCommunityPostIdAndUserId(POST_ID, USER_ID)).willReturn(false);
            given(communityPostLikeRepository.save(any())).willThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> communityPostReactionService.likePost(POST_ID, USER_ID))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostReactionException.ALREADY_LIKED.getDetail());
        }

        @Test
        void 존재하지_않는_게시글에는_좋아요를_누를_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID))
                    .willThrow(new ApplicationException(CommunityPostException.POST_NOT_FOUND));

            assertThatThrownBy(() -> communityPostReactionService.likePost(POST_ID, USER_ID))
                    .isInstanceOf(ApplicationException.class);

            then(communityPostLikeRepository).should(never()).save(any());
        }

        @Test
        void 좋아요를_취소하면_행이_삭제된다() {
            given(communityPostLikeRepository.deleteByCommunityPostIdAndUserId(POST_ID, USER_ID)).willReturn(1);

            communityPostReactionService.unlikePost(POST_ID, USER_ID);

            then(communityPostLikeRepository).should().deleteByCommunityPostIdAndUserId(POST_ID, USER_ID);
        }

        @Test
        void 좋아요한_적_없으면_취소할_수_없다() {
            given(communityPostLikeRepository.deleteByCommunityPostIdAndUserId(POST_ID, USER_ID)).willReturn(0);

            assertThatThrownBy(() -> communityPostReactionService.unlikePost(POST_ID, USER_ID))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostReactionException.LIKE_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("스크랩")
    class Scrap {

        @Test
        void 게시글을_스크랩하면_저장된다() {
            givenPost();
            given(communityPostScrapRepository.existsByCommunityPostIdAndUserId(POST_ID, USER_ID)).willReturn(false);

            communityPostReactionService.scrapPost(POST_ID, USER_ID);

            then(communityPostScrapRepository).should().save(any());
        }

        @Test
        void 이미_스크랩한_게시글은_다시_스크랩할_수_없다() {
            givenPost();
            given(communityPostScrapRepository.existsByCommunityPostIdAndUserId(POST_ID, USER_ID)).willReturn(true);

            assertThatThrownBy(() -> communityPostReactionService.scrapPost(POST_ID, USER_ID))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostReactionException.ALREADY_SCRAPPED.getDetail());

            then(communityPostScrapRepository).should(never()).save(any());
        }

        @Test
        void 동시에_두_번_눌려_유니크_제약이_걸려도_ALREADY_SCRAPPED로_응답한다() {
            givenPost();
            given(communityPostScrapRepository.existsByCommunityPostIdAndUserId(POST_ID, USER_ID)).willReturn(false);
            given(communityPostScrapRepository.save(any())).willThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> communityPostReactionService.scrapPost(POST_ID, USER_ID))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostReactionException.ALREADY_SCRAPPED.getDetail());
        }

        @Test
        void 스크랩을_취소하면_행이_삭제된다() {
            given(communityPostScrapRepository.deleteByCommunityPostIdAndUserId(POST_ID, USER_ID)).willReturn(1);

            communityPostReactionService.unscrapPost(POST_ID, USER_ID);

            then(communityPostScrapRepository).should().deleteByCommunityPostIdAndUserId(POST_ID, USER_ID);
        }

        @Test
        void 스크랩한_적_없으면_취소할_수_없다() {
            given(communityPostScrapRepository.deleteByCommunityPostIdAndUserId(POST_ID, USER_ID)).willReturn(0);

            assertThatThrownBy(() -> communityPostReactionService.unscrapPost(POST_ID, USER_ID))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostReactionException.SCRAP_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("내 스크랩 목록 조회")
    class GetMyScraps {

        @Test
        void 스크랩한_순서대로_게시글_목록을_내려준다() {
            CommunityPostScrap newerScrap = scrap(20L, 2L);
            CommunityPostScrap olderScrap = scrap(10L, 1L);
            given(communityPostScrapRepository.findPageByCursor(any(), any(), any()))
                    .willReturn(List.of(newerScrap, olderScrap));
            given(communityPostRepository.findAllById(anyList()))
                    .willReturn(List.of(post(1L, 999L), post(2L, 999L)));
            given(userRepository.findAllById(anyList())).willReturn(List.of(userWithNickname(999L, "작성자")));

            CommunityPostScrapListResult result = communityPostReactionService.getMyScraps(
                    CommunityPostScrapListCommand.of(USER_ID, null, 10));

            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).id()).isEqualTo(2L);
            assertThat(result.content().get(1).id()).isEqualTo(1L);
        }

        @Test
        void 요청_크기보다_한_건_더_있으면_hasNext가_true이고_다음_커서는_마지막_스크랩_id다() {
            List<CommunityPostScrap> threeScraps = List.of(scrap(30L, 3L), scrap(20L, 2L), scrap(10L, 1L));
            given(communityPostScrapRepository.findPageByCursor(any(), any(), any())).willReturn(threeScraps);
            given(communityPostRepository.findAllById(anyList())).willReturn(List.of(
                    post(1L, 999L), post(2L, 999L), post(3L, 999L)));
            given(userRepository.findAllById(anyList())).willReturn(List.of(userWithNickname(999L, "작성자")));

            CommunityPostScrapListResult result = communityPostReactionService.getMyScraps(
                    CommunityPostScrapListCommand.of(USER_ID, null, 2));

            assertThat(result.content()).hasSize(2);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(20L);
        }

        @Test
        void 마지막_페이지면_hasNext가_false이고_nextCursor는_null이다() {
            given(communityPostScrapRepository.findPageByCursor(any(), any(), any()))
                    .willReturn(List.of(scrap(10L, 1L)));
            given(communityPostRepository.findAllById(anyList())).willReturn(List.of(post(1L, 999L)));
            given(userRepository.findAllById(anyList())).willReturn(List.of(userWithNickname(999L, "작성자")));

            CommunityPostScrapListResult result = communityPostReactionService.getMyScraps(
                    CommunityPostScrapListCommand.of(USER_ID, null, 10));

            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        void 게시글이_삭제되어_없는_스크랩은_목록에서_조용히_빠진다() {
            given(communityPostScrapRepository.findPageByCursor(any(), any(), any()))
                    .willReturn(List.of(scrap(10L, 1L)));
            given(communityPostRepository.findAllById(anyList())).willReturn(List.of());
            given(userRepository.findAllById(anyList())).willReturn(List.of());

            CommunityPostScrapListResult result = communityPostReactionService.getMyScraps(
                    CommunityPostScrapListCommand.of(USER_ID, null, 10));

            assertThat(result.content()).isEmpty();
        }
    }
}
