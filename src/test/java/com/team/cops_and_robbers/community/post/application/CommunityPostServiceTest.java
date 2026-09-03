package com.team.cops_and_robbers.community.post.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.exception.InfrastructureException;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostRow;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostSearchCondition;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostCreateCommand;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostDeleteCommand;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostListCommand;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostStatusCommand;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostUpdateCommand;
import com.team.cops_and_robbers.community.post.application.dto.result.CommunityPostCursorResult;
import com.team.cops_and_robbers.community.post.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.post.domain.CommunityPost;
import com.team.cops_and_robbers.community.post.domain.CommunityPostScope;
import com.team.cops_and_robbers.community.post.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.post.domain.PostAddress;
import com.team.cops_and_robbers.community.post.domain.RecruitmentStatus;
import com.team.cops_and_robbers.community.post.exception.CommunityPostException;
import com.team.cops_and_robbers.community.post.infrastructure.GeocodingResult;
import com.team.cops_and_robbers.community.post.repository.CommunityPostCountProjection;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import static org.mockito.Mockito.never;

class CommunityPostServiceTest extends ServiceUnitTest {

    @InjectMocks
    private CommunityPostService communityPostService;

    @Spy
    private Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

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
            given(userRepository.getByUserId(1L)).willReturn(USER());
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
            given(userRepository.getByUserId(1L)).willReturn(USER());
            given(geocodingClient.reverseGeocode(37.4979, 127.0276)).willReturn(GeocodingResult.notFound());

            assertThatThrownBy(() -> communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "제목", "내용",
                            LocalDateTime.now().plusDays(3), 37.4979, 127.0276, "만나는곳", 6)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.ADDRESS_NOT_FOUND.getDetail());
            then(communityPostRepository).should(never()).save(any());
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
            assertThat(result.writerProfileIcon()).isEqualTo(User.DEFAULT_PROFILE_ICON);
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

        @Test
        void 생성_직후에는_좋아요_스크랩_카운트와_내_반응이_모두_비어있다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(userRepository.getByUserId(1L)).willReturn(USER());
            given(geocodingClient.reverseGeocode(37.4979, 127.0276)).willReturn(GeocodingResult.resolved(PostAddress.of(
                            "서울특별시 광진구 화양동 1-20", "서울특별시 광진구 능동로 216", null,
                            "서울특별시 광진구 화양동", "KR")));
            given(communityPostRepository.save(any())).willReturn(post);

            CommunityPostResult result = communityPostService.createPost(
                    new CommunityPostCreateCommand(1L, "제목", "내용",
                            LocalDateTime.now().plusDays(3), 37.4979, 127.0276, "만나는곳", 6));

            assertThat(result.likeCount()).isZero();
            assertThat(result.scrapCount()).isZero();
            assertThat(result.isLikedByRequester()).isFalse();
            assertThat(result.isScrappedByRequester()).isFalse();
            then(communityPostLikeRepository).shouldHaveNoInteractions();
            then(communityPostScrapRepository).shouldHaveNoInteractions();
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
                    listCommand(null, 10), null);

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
                    listCommand(null, 10), null);

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
                    listCommand(cursor, 10), null);

            assertThat(result.content()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        void 목록의_작성자_닉네임과_아이콘이_매핑되고_탈퇴한_작성자는_알수없음과_기본_아이콘으로_표시된다() {
            CommunityPost post1 = POST(1L, LocalDateTime.of(2026, 8, 1, 2, 0));
            CommunityPost post2 = POST(999L, LocalDateTime.of(2026, 8, 1, 1, 0));
            setId(post1, 1L);
            setId(post2, 2L);
            given(communityPostRepository.findPage(any(CommunityPostSearchCondition.class), eq(null), eq(10)))
                    .willReturn(List.of(new CommunityPostRow(post1, null, null), new CommunityPostRow(post2, null, null)));
            given(userRepository.findAllById(List.of(1L, 999L)))
                    .willReturn(List.of(userWithId(1L, "무서운경찰관", 2)));

            CommunityPostCursorResult result = communityPostService.getPostList(
                    listCommand(null, 10), null);

            assertThat(result.content().get(0).writerNickname()).isEqualTo("무서운경찰관");
            assertThat(result.content().get(0).writerProfileIcon()).isEqualTo(2);
            assertThat(result.content().get(1).writerNickname()).isEqualTo("알수없음");
            assertThat(result.content().get(1).writerProfileIcon()).isEqualTo(User.DEFAULT_PROFILE_ICON);
        }

        @Test
        void 잘못된_커서면_INVALID_QUERY_PARAMETER_예외가_발생한다() {
            assertThatThrownBy(() -> communityPostService.getPostList(
                    listCommand("broken!!", 10), null))
                    .isInstanceOf(ApplicationException.class);
        }

        @Test
        void 로그인_사용자가_조회하면_목록에_좋아요_스크랩_카운트와_내_반응이_배치로_반영된다() {
            CommunityPost post1 = POST(1L, LocalDateTime.of(2026, 8, 1, 2, 0));
            CommunityPost post2 = POST(1L, LocalDateTime.of(2026, 8, 1, 1, 0));
            setId(post1, 1L);
            setId(post2, 2L);
            given(communityPostRepository.findPage(any(CommunityPostSearchCondition.class), eq(null), eq(10)))
                    .willReturn(List.of(new CommunityPostRow(post1, null, null), new CommunityPostRow(post2, null, null)));
            given(userRepository.findAllById(List.of(1L))).willReturn(List.of(userWithId(1L, "무서운경찰관")));
            given(communityPostLikeRepository.countByPostIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(new CommunityPostCountProjection(1L, 3L)));
            given(communityPostScrapRepository.countByPostIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(new CommunityPostCountProjection(2L, 1L)));
            given(communityPostLikeRepository.findLikedPostIds(5L, List.of(1L, 2L))).willReturn(List.of(2L));
            given(communityPostScrapRepository.findScrappedPostIds(5L, List.of(1L, 2L))).willReturn(List.of(1L));

            CommunityPostCursorResult result = communityPostService.getPostList(listCommand(null, 10), 5L);

            CommunityPostResult first = result.content().get(0);
            CommunityPostResult second = result.content().get(1);
            assertThat(first.id()).isEqualTo(1L);
            assertThat(first.likeCount()).isEqualTo(3L);
            assertThat(first.scrapCount()).isEqualTo(0L);
            assertThat(first.isLikedByRequester()).isFalse();
            assertThat(first.isScrappedByRequester()).isTrue();
            assertThat(second.id()).isEqualTo(2L);
            assertThat(second.likeCount()).isEqualTo(0L);
            assertThat(second.scrapCount()).isEqualTo(1L);
            assertThat(second.isLikedByRequester()).isTrue();
            assertThat(second.isScrappedByRequester()).isFalse();
        }

        @Test
        void 비로그인_조회는_목록의_liked_scrapped가_모두_false이고_배치_exists_조회를_하지_않는다() {
            CommunityPost post1 = POST(1L, LocalDateTime.of(2026, 8, 1, 2, 0));
            setId(post1, 1L);
            given(communityPostRepository.findPage(any(CommunityPostSearchCondition.class), eq(null), eq(10)))
                    .willReturn(List.of(new CommunityPostRow(post1, null, null)));
            given(userRepository.findAllById(List.of(1L))).willReturn(List.of(userWithId(1L, "무서운경찰관")));

            CommunityPostCursorResult result = communityPostService.getPostList(listCommand(null, 10), null);

            assertThat(result.content().get(0).isLikedByRequester()).isFalse();
            assertThat(result.content().get(0).isScrappedByRequester()).isFalse();
            then(communityPostLikeRepository).should(never()).findLikedPostIds(any(), any());
            then(communityPostScrapRepository).should(never()).findScrappedPostIds(any(), any());
        }

        @Test
        void 국가_코드가_없으면_COUNTRY_NOT_SPECIFIED_예외가_발생한다() {
            assertThatThrownBy(() -> new CommunityPostListCommand(
                    null, 10, CommunityPostScope.ALL, CommunityPostSort.LATEST, null, null, null, null, null))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.COUNTRY_NOT_SPECIFIED.getDetail());
        }

        @Test
        void 국가와_제외_국가를_함께_주면_CONFLICTING_COUNTRY_FILTER_예외가_발생한다() {
            assertThatThrownBy(() -> new CommunityPostListCommand(
                    null, 10, CommunityPostScope.ALL, CommunityPostSort.LATEST,
                    "KR", List.of("JP"), null, null, null))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.CONFLICTING_COUNTRY_FILTER.getDetail());
        }

        @Test
        void 제외_국가는_대문자로_정렬해_담기므로_순서가_달라도_같은_커서_키를_만든다() {
            CommunityPostListCommand ascending = excludeCommand(List.of("jp", "kr"));
            CommunityPostListCommand descending = excludeCommand(List.of("KR", "JP"));

            assertThat(ascending.countryScopeKey()).isEqualTo("!JP,KR");
            assertThat(descending.countryScopeKey()).isEqualTo(ascending.countryScopeKey());
        }

        @Test
        void 제외_국가만_주면_countryCode는_비고_국가_조회_커서_키와_겹치지_않는다() {
            CommunityPostListCommand command = excludeCommand(List.of("KR"));

            assertThat(command.countryCode()).isNull();
            assertThat(command.countryScopeKey()).isEqualTo("!KR");
            assertThat(listCommand(null, 10).countryScopeKey()).isEqualTo("KR");
        }

        @Test
        void 빈_값만_담긴_제외_국가는_국가를_지정하지_않은_것으로_본다() {
            assertThatThrownBy(() -> new CommunityPostListCommand(
                    null, 10, CommunityPostScope.ALL, CommunityPostSort.LATEST,
                    null, List.of("", " "), null, null, null))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.COUNTRY_NOT_SPECIFIED.getDetail());
        }

        @Test
        void 국가_코드_형식이_아니면_INVALID_QUERY_PARAMETER_예외가_발생한다() {
            assertThatThrownBy(() -> excludeCommand(List.of("A|B")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommonException.INVALID_QUERY_PARAMETER.getDetail());
            assertThatThrownBy(() -> excludeCommand(List.of("KOR")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommonException.INVALID_QUERY_PARAMETER.getDetail());
            assertThatThrownBy(() -> new CommunityPostListCommand(
                    null, 10, CommunityPostScope.ALL, CommunityPostSort.LATEST,
                    "K|R", null, null, null, null))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommonException.INVALID_QUERY_PARAMETER.getDetail());
        }

        private CommunityPostListCommand excludeCommand(List<String> excludeCountryCodes) {
            return new CommunityPostListCommand(
                    null, 10, CommunityPostScope.ALL, CommunityPostSort.LATEST,
                    null, excludeCountryCodes, null, null, null);
        }

        private CommunityPostListCommand listCommand(String cursor, int size) {
            return new CommunityPostListCommand(
                    cursor, size, CommunityPostScope.ALL, CommunityPostSort.LATEST, "KR", null, null, null, null);
        }

        private List<CommunityPostRow> postsOf(int count) {
            List<CommunityPostRow> rows = new ArrayList<>();
            for (int i = count; i >= 1; i--) {
                CommunityPost post = POST(1L, LocalDateTime.of(2026, 8, 1, 0, 0).plusHours(i));
                setId(post, (long) i);
                rows.add(new CommunityPostRow(post, null, null));
            }
            return rows;
        }

        private User userWithId(Long id, String nickname) {
            User user = USER(nickname);
            setId(user, id);
            return user;
        }

        private User userWithId(Long id, String nickname, int profileIcon) {
            User user = userWithId(id, nickname);
            ReflectionTestUtils.setField(user, "profileIcon", profileIcon);
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

            CommunityPostResult result = communityPostService.getPost(1L, 2L);

            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        void 비로그인_조회는_chatJoined가_false다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);

            CommunityPostResult result = communityPostService.getPost(1L, null);

            assertThat(result.chatJoined()).isFalse();
            then(communityChatMemberRepository).should(never()).existsByCommunityPostIdAndUserId(any(), any());
        }

        @Test
        void 로그인했고_채팅방_멤버면_chatJoined가_true다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(1L, 2L)).willReturn(true);

            CommunityPostResult result = communityPostService.getPost(1L, 2L);

            assertThat(result.chatJoined()).isTrue();
        }

        @Test
        void 로그인했지만_채팅방_멤버가_아니면_chatJoined가_false다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);
            given(communityChatMemberRepository.existsByCommunityPostIdAndUserId(1L, 2L)).willReturn(false);

            CommunityPostResult result = communityPostService.getPost(1L, 2L);

            assertThat(result.chatJoined()).isFalse();
        }

        @Test
        void 존재하지_않는_ID로_조회하면_POST_NOT_FOUND_예외가_발생한다() {
            given(communityPostRepository.getByPostId(999L))
                    .willThrow(new ApplicationException(CommunityPostException.POST_NOT_FOUND));

            assertThatThrownBy(() -> communityPostService.getPost(999L, 1L))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.POST_NOT_FOUND.getDetail());
        }

        @Test
        void 좋아요_스크랩_카운트와_내가_눌렀는지_여부가_반영된다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);
            given(communityPostLikeRepository.countByCommunityPostId(1L)).willReturn(5L);
            given(communityPostScrapRepository.countByCommunityPostId(1L)).willReturn(2L);
            given(communityPostLikeRepository.existsByCommunityPostIdAndUserId(1L, 2L)).willReturn(true);
            given(communityPostScrapRepository.existsByCommunityPostIdAndUserId(1L, 2L)).willReturn(false);

            CommunityPostResult result = communityPostService.getPost(1L, 2L);

            assertThat(result.likeCount()).isEqualTo(5L);
            assertThat(result.scrapCount()).isEqualTo(2L);
            assertThat(result.isLikedByRequester()).isTrue();
            assertThat(result.isScrappedByRequester()).isFalse();
        }

        @Test
        void 비로그인_조회는_liked_scrapped가_false이고_카운트는_그대로_내려간다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);
            given(communityPostLikeRepository.countByCommunityPostId(1L)).willReturn(5L);
            given(communityPostScrapRepository.countByCommunityPostId(1L)).willReturn(2L);

            CommunityPostResult result = communityPostService.getPost(1L, null);

            assertThat(result.likeCount()).isEqualTo(5L);
            assertThat(result.scrapCount()).isEqualTo(2L);
            assertThat(result.isLikedByRequester()).isFalse();
            assertThat(result.isScrappedByRequester()).isFalse();
            then(communityPostLikeRepository).should(never()).existsByCommunityPostIdAndUserId(any(), any());
            then(communityPostScrapRepository).should(never()).existsByCommunityPostIdAndUserId(any(), any());
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
            given(userRepository.getByUserId(1L)).willReturn(USER());
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
            given(userRepository.getByUserId(1L)).willReturn(USER());
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
            given(userRepository.getByUserId(1L)).willReturn(USER());
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
            given(userRepository.getByUserId(1L)).willReturn(USER());

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
            given(userRepository.getByUserId(1L)).willReturn(USER());
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
            given(userRepository.getByUserId(1L)).willReturn(USER());
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

        @Test
        void 수정_응답에도_기존_좋아요_스크랩_카운트가_유지된다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostId(1L)).willReturn(post);
            given(geocodingClient.reverseGeocode(37.5665, 126.9780)).willReturn(GeocodingResult.resolved(PostAddress.of(
                            "서울특별시 광진구 화양동 1-20", "서울특별시 광진구 능동로 216", null,
                            "서울특별시 광진구 화양동", "KR")));
            given(communityPostLikeRepository.countByCommunityPostId(1L)).willReturn(4L);
            given(communityPostScrapRepository.countByCommunityPostId(1L)).willReturn(1L);

            CommunityPostResult result = communityPostService.updatePost(
                    new CommunityPostUpdateCommand(1L, 1L, "수정된 제목", "수정된 내용",
                            LocalDateTime.now().plusDays(5), 37.5665, 126.9780, "만나는곳", 8));

            assertThat(result.likeCount()).isEqualTo(4L);
            assertThat(result.scrapCount()).isEqualTo(1L);
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
        void 게시글을_삭제하면_고정_공지도_함께_정리된다() {
            CommunityPost post = POST(1L);
            setId(post, 1L);
            given(communityPostRepository.getByPostIdForUpdate(1L)).willReturn(post);

            communityPostService.deletePost(new CommunityPostDeleteCommand(1L, 1L));

            then(communityChatPinRepository).should().deleteByCommunityPostId(1L);
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
