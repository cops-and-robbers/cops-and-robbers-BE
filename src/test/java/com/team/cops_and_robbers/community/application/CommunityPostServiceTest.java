package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.InfrastructureException;
import com.team.cops_and_robbers.community.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.application.dto.CommunityPostRow;
import com.team.cops_and_robbers.community.application.dto.CommunityPostSearchCondition;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostCreateCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostDeleteCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostListCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostStatusCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostUpdateCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostCursorResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.CommunityPostScope;
import com.team.cops_and_robbers.community.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.domain.PostAddress;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.infrastructure.GeocodingResult;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
            given(geocodingClient.reverseGeocode(37.4979, 127.0276)).willReturn(GeocodingResult.resolved(PostAddress.of(
                            "서울특별시 광진구 화양동 1-20", "서울특별시 광진구 능동로 216", null,
                            "서울특별시 광진구 화양동", "KR")));
            given(communityPostRepository.save(any())).willReturn(post);

            CommunityPostResult result = communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "같이 경찰과 도둑 하실 분!", "강남역 근처에서 5명 모집합니다.",
                            LocalDateTime.now().plusDays(3), 37.4979, 127.0276, "만나는곳", 6));

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
            given(geocodingClient.reverseGeocode(37.4979, 127.0276)).willReturn(GeocodingResult.resolved(PostAddress.of(
                            "서울특별시 광진구 화양동 1-20", "서울특별시 광진구 능동로 216", null,
                            "서울특별시 광진구 화양동", "KR")));
            given(communityPostRepository.save(any())).willReturn(post);

            communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "같이 경찰과 도둑 하실 분!", "강남역 근처에서 5명 모집합니다.",
                            LocalDateTime.now().plusDays(3), 37.4979, 127.0276, "만나는곳", 6));

            then(communityChatMemberRepository).should().save(any());
        }

        @Test
        void 게시글_생성_시_역지오코딩된_주소가_저장된다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(userRepository.getByUserId(1L)).willReturn(USER());
            given(geocodingClient.reverseGeocode(37.4979, 127.0276))
                    .willReturn(GeocodingResult.resolved(
                            PostAddress.of("서울 강남구 역삼동", "서울 강남구 테헤란로 152", "강남파이낸스센터", "서울 강남구 역삼동", "KR")));
            given(communityPostRepository.save(any())).willReturn(post);

            communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "제목", "내용",
                            LocalDateTime.now().plusDays(3), 37.4979, 127.0276, "만나는곳", 6));

            ArgumentCaptor<CommunityPost> captor = ArgumentCaptor.forClass(CommunityPost.class);
            then(communityPostRepository).should().save(captor.capture());
            assertThat(captor.getValue().getAddress()).isEqualTo("서울 강남구 역삼동");
            assertThat(captor.getValue().getRoadAddress()).isEqualTo("서울 강남구 테헤란로 152");
            assertThat(captor.getValue().getBuildingName()).isEqualTo("강남파이낸스센터");
        }

        @Test
        void 역지오코딩이_실패하면_게시글을_만들지_않는다() {
            given(geocodingClient.reverseGeocode(37.4979, 127.0276)).willReturn(GeocodingResult.failed());

            assertThatThrownBy(() -> communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "제목", "내용",
                            LocalDateTime.now().plusDays(3), 37.4979, 127.0276, "만나는곳", 6)))
                    .isInstanceOf(InfrastructureException.class)
                    .hasMessageContaining(CommunityPostException.ADDRESS_LOOKUP_FAILED.getDetail());
            then(communityPostRepository).shouldHaveNoInteractions();
        }

        @Test
        void 주소를_찾을_수_없는_위치면_ADDRESS_NOT_FOUND_예외가_발생한다() {
            given(geocodingClient.reverseGeocode(37.4979, 127.0276)).willReturn(GeocodingResult.notFound());

            assertThatThrownBy(() -> communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "제목", "내용",
                            LocalDateTime.now().plusDays(3), 37.4979, 127.0276, "만나는곳", 6)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.ADDRESS_NOT_FOUND.getDetail());
            then(userRepository).shouldHaveNoInteractions();
        }

        @Test
        void 생성_응답에_작성자_닉네임이_포함된다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(userRepository.getByUserId(1L)).willReturn(USER("무서운경찰관"));
            given(geocodingClient.reverseGeocode(37.4979, 127.0276)).willReturn(GeocodingResult.resolved(PostAddress.of(
                            "서울특별시 광진구 화양동 1-20", "서울특별시 광진구 능동로 216", null,
                            "서울특별시 광진구 화양동", "KR")));
            given(communityPostRepository.save(any())).willReturn(post);

            CommunityPostResult result = communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "제목", "내용",
                            LocalDateTime.now().plusDays(3), 37.4979, 127.0276, "만나는곳", 6));

            assertThat(result.writerNickname()).isEqualTo("무서운경찰관");
        }

        @Test
        void 과거_모임_날짜로_생성하면_INVALID_MEETING_DATE_예외가_발생한다() {
            assertThatThrownBy(() -> communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "제목", "내용",
                            LocalDateTime.now().minusDays(1), 37.4979, 127.0276, "만나는곳", 6)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.INVALID_MEETING_DATE.getDetail());
            then(geocodingClient).shouldHaveNoInteractions();
            then(userRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("게시글 목록 커서 조회")
    class GetPostList {

        @Test
        void 커서가_없으면_첫_페이지와_다음_커서를_반환한다() {
            given(communityPostRepository.findPage(any(CommunityPostSearchCondition.class), eq(null), eq(10)))
                    .willReturn(postsOf(11));
            given(userRepository.findAllById(List.of(1L))).willReturn(List.of(userWithId(1L, "무서운경찰관")));

            CommunityPostCursorResult result = communityPostService.getPostList(
                    listCommand(null, 10));

            assertThat(result.content()).hasSize(10);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor())
                    .isEqualTo(CommunityPostCursor.encode("KR", CommunityPostSort.LATEST, null, false, CommunityPostCursor.sortKeyOf(LocalDateTime.of(2026, 8, 1, 0, 0).plusHours(2)), 2L));
        }

        @Test
        void 마지막_페이지면_hasNext가_false이고_커서가_null이다() {
            given(communityPostRepository.findPage(any(CommunityPostSearchCondition.class), eq(null), eq(10)))
                    .willReturn(postsOf(3));
            given(userRepository.findAllById(List.of(1L))).willReturn(List.of(userWithId(1L, "무서운경찰관")));

            CommunityPostCursorResult result = communityPostService.getPostList(
                    listCommand(null, 10));

            assertThat(result.content()).hasSize(3);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        void 커서가_있으면_디코딩한_값으로_조회한다() {
            LocalDateTime cursorCreatedAt = LocalDateTime.of(2026, 8, 1, 5, 0);
            String cursor = CommunityPostCursor.encode("KR", CommunityPostSort.LATEST, null, false, CommunityPostCursor.sortKeyOf(cursorCreatedAt), 5L);
            given(communityPostRepository.findPage(any(CommunityPostSearchCondition.class), any(CommunityPostCursor.class), eq(10)))
                    .willReturn(List.of());

            CommunityPostCursorResult result = communityPostService.getPostList(
                    listCommand(cursor, 10));

            assertThat(result.content()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        void 목록의_작성자_닉네임이_매핑되고_탈퇴한_작성자는_탈퇴한_사용자로_표시된다() {
            CommunityPost post1 = POST(1L, LocalDateTime.of(2026, 8, 1, 2, 0));
            CommunityPost post2 = POST(999L, LocalDateTime.of(2026, 8, 1, 1, 0));
            setId(post1, 1L);
            setId(post2, 2L);
            given(communityPostRepository.findPage(any(CommunityPostSearchCondition.class), eq(null), eq(10)))
                    .willReturn(List.of(new CommunityPostRow(post1, null), new CommunityPostRow(post2, null)));
            given(userRepository.findAllById(List.of(1L, 999L)))
                    .willReturn(List.of(userWithId(1L, "무서운경찰관")));

            CommunityPostCursorResult result = communityPostService.getPostList(
                    listCommand(null, 10));

            assertThat(result.content().get(0).writerNickname()).isEqualTo("무서운경찰관");
            assertThat(result.content().get(1).writerNickname()).isEqualTo("탈퇴한 사용자");
        }

        @Test
        void 잘못된_커서면_INVALID_QUERY_PARAMETER_예외가_발생한다() {
            assertThatThrownBy(() -> communityPostService.getPostList(
                    listCommand("broken!!", 10)))
                    .isInstanceOf(ApplicationException.class);
        }



        @Test
        void 국가_코드가_없으면_COUNTRY_NOT_SPECIFIED_예외가_발생한다() {
            assertThatThrownBy(() -> new CommunityPostListCommand(
                    null, 10, CommunityPostScope.ALL, CommunityPostSort.LATEST, null, null, null, null))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.COUNTRY_NOT_SPECIFIED.getDetail());
        }


        private CommunityPostListCommand listCommand(String cursor, int size) {
            return new CommunityPostListCommand(
                    cursor, size, CommunityPostScope.ALL, CommunityPostSort.LATEST, "KR", null, null, null);
        }

        private List<CommunityPostRow> postsOf(int count) {
            List<CommunityPostRow> rows = new ArrayList<>();
            for (int i = count; i >= 1; i--) {
                CommunityPost post = POST(1L, LocalDateTime.of(2026, 8, 1, 0, 0).plusHours(i));
                setId(post, (long) i);
                rows.add(new CommunityPostRow(post, null));
            }
            return rows;
        }

        private User userWithId(Long id, String nickname) {
            User user = USER(nickname);
            setId(user, id);
            return user;
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
            given(geocodingClient.reverseGeocode(37.5665, 126.9780)).willReturn(GeocodingResult.resolved(PostAddress.of(
                            "서울특별시 광진구 화양동 1-20", "서울특별시 광진구 능동로 216", null,
                            "서울특별시 광진구 화양동", "KR")));

            CommunityPostResult result = communityPostService.updatePost(
                    new CommunityPostUpdateCommand(1L, 1L, "수정된 제목", "수정된 내용",
                            LocalDateTime.now().plusDays(5), 37.5665, 126.9780, "만나는곳", 8));

            assertThat(result.title()).isEqualTo("수정된 제목");
            assertThat(result.content()).isEqualTo("수정된 내용");
            assertThat(result.maxParticipants()).isEqualTo(8);
        }

        @Test
        void 좌표가_변경되면_주소를_재변환한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);
            given(geocodingClient.reverseGeocode(37.5665, 126.9780))
                    .willReturn(GeocodingResult.resolved(
                            PostAddress.of("서울 중구 태평로1가", "서울특별시 중구 세종대로 110", "서울시청", "서울 중구 태평로1가", "KR")));

            communityPostService.updatePost(new CommunityPostUpdateCommand(
                    1L, 1L, "제목", "내용", LocalDateTime.now().plusDays(3), 37.5665, 126.9780, "만나는곳", 6));

            assertThat(post.getAddress()).isEqualTo("서울 중구 태평로1가");
            assertThat(post.getRoadAddress()).isEqualTo("서울특별시 중구 세종대로 110");
            assertThat(post.getBuildingName()).isEqualTo("서울시청");
        }

        @Test
        void 좌표가_변경됐는데_역지오코딩이_실패하면_수정하지_않는다() {
            CommunityPost post = POST(1L, "서울 강남구 역삼동");
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);
            given(geocodingClient.reverseGeocode(37.5665, 126.9780)).willReturn(GeocodingResult.failed());

            assertThatThrownBy(() -> communityPostService.updatePost(new CommunityPostUpdateCommand(
                    1L, 1L, "새 제목", "새 내용", LocalDateTime.now().plusDays(3), 37.5665, 126.9780, "만나는곳", 6)))
                    .isInstanceOf(InfrastructureException.class);
        }

        @Test
        void 좌표와_주소가_모두_그대로면_역지오코딩을_호출하지_않는다() {
            CommunityPost post = POST(1L, "서울 강남구 역삼동");
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);

            communityPostService.updatePost(new CommunityPostUpdateCommand(
                    1L, 1L, "새 제목", "새 내용", LocalDateTime.now().plusDays(3), 37.4979, 127.0276, "만나는곳", 6));

            then(geocodingClient).shouldHaveNoInteractions();
        }

        @Test
        void 좌표가_그대로여도_주소가_비어있으면_역지오코딩을_다시_시도한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            ReflectionTestUtils.setField(post, "countryCode", null);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);
            given(geocodingClient.reverseGeocode(37.4979, 127.0276))
                    .willReturn(GeocodingResult.resolved(
                            PostAddress.of("서울 강남구 역삼동", "서울 강남구 테헤란로 152", "강남파이낸스센터", "서울 강남구 역삼동", "KR")));

            communityPostService.updatePost(new CommunityPostUpdateCommand(
                    1L, 1L, "새 제목", "새 내용", LocalDateTime.now().plusDays(3), 37.4979, 127.0276, "만나는곳", 6));

            assertThat(post.getAddress()).isEqualTo("서울 강남구 역삼동");
        }

        @Test
        void 주소를_찾을_수_없는_위치로_수정하면_ADDRESS_NOT_FOUND_예외가_발생한다() {
            CommunityPost post = POST(1L, "서울 강남구 역삼동");
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);
            given(geocodingClient.reverseGeocode(37.5665, 126.9780)).willReturn(GeocodingResult.notFound());

            assertThatThrownBy(() -> communityPostService.updatePost(new CommunityPostUpdateCommand(
                    1L, 1L, "제목", "내용", LocalDateTime.now().plusDays(3), 37.5665, 126.9780, "만나는곳", 6)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.ADDRESS_NOT_FOUND.getDetail());
        }

        @Test
        void 작성자가_아닌_사용자가_수정하면_FORBIDDEN_NOT_AUTHOR_예외가_발생한다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);

            assertThatThrownBy(() -> communityPostService.updatePost(
                    new CommunityPostUpdateCommand(999L, 1L, "수정된 제목", "수정된 내용",
                            LocalDateTime.now().plusDays(5), 37.5665, 126.9780, "만나는곳", 8)))
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
                            LocalDateTime.now().minusDays(1), 37.5665, 126.9780, "만나는곳", 8)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.INVALID_MEETING_DATE.getDetail());
        }

        @Test
        void 존재하지_않는_ID로_수정하면_POST_NOT_FOUND_예외가_발생한다() {
            given(communityPostRepository.getByPostId(999L))
                    .willThrow(new ApplicationException(CommunityPostException.POST_NOT_FOUND));

            assertThatThrownBy(() -> communityPostService.updatePost(
                    new CommunityPostUpdateCommand(1L, 999L, "수정된 제목", "수정된 내용",
                            LocalDateTime.now().plusDays(5), 37.5665, 126.9780, "만나는곳", 8)))
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
        void 게시글을_삭제하면_댓글과_좋아요와_스크랩도_함께_정리된다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostIdForUpdate(1L)).willReturn(post);

            communityPostService.deletePost(new CommunityPostDeleteCommand(1L, 1L));

            then(communityCommentRepository).should().deleteAllByCommunityPostId(1L);
            then(communityPostLikeRepository).should().deleteAllByCommunityPostId(1L);
            then(communityPostScrapRepository).should().deleteAllByCommunityPostId(1L);
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
