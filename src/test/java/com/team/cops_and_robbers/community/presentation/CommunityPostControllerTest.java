package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityPostLike;
import com.team.cops_and_robbers.community.domain.CommunityPostNotificationSetting;
import com.team.cops_and_robbers.community.domain.CommunityPostScrap;
import com.team.cops_and_robbers.community.domain.PostAddress;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.infrastructure.GeocodingResult;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostStatusRequest;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.COMPLETED_POST;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.PAST_COMPLETED_POST;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.PAST_POST;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST_AT;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST_IN_COUNTRY;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST_TITLED;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST_PLACED;
import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST_MEETING_IN;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

class CommunityPostControllerTest extends ControllerTest {

    private static final String POST_API_URL = "/api/community-posts";
    private static final String ADDRESS_API_URL = POST_API_URL + "/address";
    private static final String COUNTRY_API_URL = POST_API_URL + "/country";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;
    private String accessToken;
    private String otherAccessToken;

    @BeforeEach
    void setUpData() {
        user = givenUser();
        accessToken = givenAccessToken(user);
        otherAccessToken = givenAccessToken(givenUser("다른유저"));
    }

    @Nested
    @DisplayName("좌표 국가 조회 API")
    class GetCountry {

        @Test
        void 좌표의_국가_코드를_200으로_응답한다() {
            doReturn(GeocodingResult.resolved(PostAddress.of(
                    "吉祥寺大通り", null, null, "東京 武蔵野市 吉祥寺本町", "JP")))
                    .when(geocodingClient)
                    .findCountry(any(), any());

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("latitude", 35.7022)
                    .queryParam("longitude", 139.5803)
                    .when()
                    .get(COUNTRY_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("countryCode")).isEqualTo("JP");
            });
        }

        @Test
        void 국가를_알_수_없는_좌표는_400을_응답한다() {
            doReturn(GeocodingResult.notFound()).when(geocodingClient).findCountry(any(), any());

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("latitude", 0.0)
                    .queryParam("longitude", 0.0)
                    .when()
                    .get(COUNTRY_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }
    }

    @Nested
    @DisplayName("좌표 주소 조회 API")
    class GetAddress {

        @Test
        void 좌표_주소_조회에_성공하면_지역과_지번을_함께_200으로_응답한다() {
            doReturn(GeocodingResult.resolved(PostAddress.of(
                    "서울특별시 광진구 화양동 1-20", "서울특별시 광진구 능동로 216", "세종대학교",
                    "서울특별시 광진구 화양동", "KR")))
                    .when(geocodingClient)
                    .reverseGeocode(any(), any());

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .queryParam("latitude", 37.5502)
                    .queryParam("longitude", 127.0736)
                    .when()
                    .get(ADDRESS_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("region")).isEqualTo("서울특별시 광진구 화양동");
                softly.assertThat(extract.jsonPath().getString("address")).isEqualTo("서울특별시 광진구 화양동 1-20");
            });
        }

        @Test
        void 주소가_없는_좌표로_조회하면_400을_응답한다() {
            doReturn(GeocodingResult.notFound()).when(geocodingClient).reverseGeocode(any(), any());

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .queryParam("latitude", 0.0)
                    .queryParam("longitude", 0.0)
                    .when()
                    .get(ADDRESS_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 지오코딩_호출이_실패하면_500을_응답한다() {
            doReturn(GeocodingResult.failed()).when(geocodingClient).reverseGeocode(any(), any());

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .queryParam("latitude", 37.5502)
                    .queryParam("longitude", 127.0736)
                    .when()
                    .get(ADDRESS_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(500);
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("latitude", 37.5502)
                    .queryParam("longitude", 127.0736)
                    .when()
                    .get(ADDRESS_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }

        @Test
        void 지원하지_않는_쿼리_파라미터로_요청하면_400을_응답한다() {
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .queryParam("latitude", 37.5502)
                    .queryParam("longitude", 127.0736)
                    .queryParam("keyword", "세종대")
                    .when()
                    .get(ADDRESS_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }
    }

    @Nested
    @DisplayName("게시글 생성 API")
    class CreatePost {

        @Test
        void 사용자가_게시글_생성에_성공하면_201을_응답한다() {
            Map<String, Object> location = Map.of("latitude", 37.4979, "longitude", 127.0276, "placeName", "강남역 11번 출구");
            Map<String, Object> request = Map.of(
                    "title", "같이 경찰과 도둑 하실 분!",
                    "content", "강남역 근처에서 5명 모집합니다.",
                    "meetingAt", LocalDateTime.now().plusDays(3).toString(),
                    "location", location,
                    "maxParticipants", 6
            );

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(201);
                softly.assertThat(extract.jsonPath().getLong("id")).isNotNull();
                softly.assertThat(extract.jsonPath().getString("title")).isEqualTo("같이 경찰과 도둑 하실 분!");
                softly.assertThat(extract.jsonPath().getString("content")).isEqualTo("강남역 근처에서 5명 모집합니다.");
                softly.assertThat(extract.jsonPath().getInt("maxParticipants")).isEqualTo(6);
                softly.assertThat(extract.jsonPath().getString("status")).isEqualTo("RECRUITING");
                softly.assertThat(extract.jsonPath().getBoolean("chatJoined")).isTrue();
            });
        }

        @Test
        void 역지오코딩에_성공하면_지역과_사용자_입력_장소를_함께_응답한다() {
            doReturn(GeocodingResult.resolved(
                    PostAddress.of("서울특별시 광진구 군자동 98", null, null, "서울특별시 광진구 군자동", "KR")))
                    .when(geocodingClient)
                    .reverseGeocode(any(), any());
            Map<String, Object> location = Map.of("latitude", 37.5502, "longitude", 127.0736, "placeName", "세종대 정문");
            Map<String, Object> request = Map.of(
                    "title", "세종대에서 하실 분!",
                    "content", "세종대 정문에서 모입니다.",
                    "meetingAt", LocalDateTime.now().plusDays(3).toString(),
                    "location", location,
                    "maxParticipants", 6
            );

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(201);
                softly.assertThat(extract.jsonPath().getString("location.region"))
                        .isEqualTo("서울특별시 광진구 군자동");
                softly.assertThat(extract.jsonPath().getString("location.placeName"))
                        .isEqualTo("세종대 정문");
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            Map<String, Object> location = Map.of("latitude", 37.4979, "longitude", 127.0276, "placeName", "강남역 11번 출구");
            Map<String, Object> request = Map.of(
                    "title", "제목",
                    "content", "내용",
                    "meetingAt", LocalDateTime.now().plusDays(3).toString(),
                    "location", location,
                    "maxParticipants", 6
            );

            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }

    @Nested
    @DisplayName("게시글 목록 조회 API")
    class GetAllPosts {

        @Test
        void 커서_없이_조회하면_첫_페이지와_다음_커서로_200을_응답한다() {
            User writer = givenUser("무서운경찰관");
            for (int i = 0; i < 15; i++) {
                communityPostRepository.save(POST(writer.getId()));
            }

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content")).hasSize(10);
                softly.assertThat(extract.jsonPath().getBoolean("cursor.hasNext")).isTrue();
                softly.assertThat(extract.jsonPath().getString("cursor.nextCursor")).isNotBlank();
                softly.assertThat(extract.jsonPath().getString("content[0].writerNickname")).isEqualTo("무서운경찰관");
                softly.assertThat(extract.jsonPath().getInt("content[0].writerProfileIcon")).isEqualTo(writer.getProfileIcon());
                softly.assertThat(extract.jsonPath().getString("content[0].location.region")).isNull();
                softly.assertThat(extract.jsonPath().getString("content[0].location.placeName"))
                        .isEqualTo("어린이대공원 정문");
            });
        }

        @Test
        void 다른_국가의_게시글은_목록에_나오지_않는다() {
            Long korea = communityPostRepository.save(POST_IN_COUNTRY(user.getId(), "KR")).getId();
            communityPostRepository.save(POST_IN_COUNTRY(user.getId(), "JP"));

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content.id", Long.class))
                        .containsExactly(korea);
            });
        }

        @Test
        void 다른_국가로_받은_커서로_요청하면_400을_응답한다() {
            for (int i = 0; i < 3; i++) {
                communityPostRepository.save(POST_IN_COUNTRY(user.getId(), "KR"));
            }
            String koreaCursor = unauthenticated()
                    .queryParam("size", 1)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract()
                    .jsonPath().getString("cursor.nextCursor");

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 1)
                    .queryParam("cursor", koreaCursor)
                    .queryParam("countryCode", "JP")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 마감된_게시글은_모집중인_게시글보다_뒤에_온다() {
            Long completedId = communityPostRepository.save(COMPLETED_POST(user.getId())).getId();
            Long endedId = communityPostRepository.save(PAST_POST(user.getId())).getId();
            Long recruitingId = communityPostRepository.save(POST(user.getId())).getId();

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            List<Long> ids = extract.jsonPath().getList("content.id", Long.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(ids).hasSize(3);
                softly.assertThat(ids.getFirst()).isEqualTo(recruitingId);
                softly.assertThat(ids.subList(1, 3)).containsExactlyInAnyOrder(completedId, endedId);
            });
        }

        @Test
        void 마감임박순은_모임_날짜가_가까운_순서로_조회한다() {
            Long far = communityPostRepository.save(POST_MEETING_IN(user.getId(), 10)).getId();
            Long near = communityPostRepository.save(POST_MEETING_IN(user.getId(), 1)).getId();
            Long middle = communityPostRepository.save(POST_MEETING_IN(user.getId(), 5)).getId();

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "DEADLINE")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content.id", Long.class))
                        .containsExactly(near, middle, far);
            });
        }

        @Test
        void 마감임박순_커서로_다음_페이지를_중복과_누락_없이_조회한다() {
            for (int i = 1; i <= 6; i++) {
                communityPostRepository.save(POST_MEETING_IN(user.getId(), i));
            }

            ExtractableResponse<Response> firstExtract = unauthenticated()
                    .queryParam("size", 4)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "DEADLINE")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();
            String nextCursor = firstExtract.jsonPath().getString("cursor.nextCursor");

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 4)
                    .queryParam("cursor", nextCursor)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "DEADLINE")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            List<Long> firstIds = firstExtract.jsonPath().getList("content.id", Long.class);
            List<Long> secondIds = extract.jsonPath().getList("content.id", Long.class);
            assertSoftly(softly -> {
                softly.assertThat(firstIds).hasSize(4);
                softly.assertThat(secondIds).hasSize(2);
                softly.assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
            });
        }

        @Test
        void 검색어가_제목에_있으면_조회된다() {
            Long target = communityPostRepository.save(
                    POST_TITLED(user.getId(), "어린이대공원에서 겜할사람~")).getId();
            communityPostRepository.save(POST_PLACED(user.getId(), "서울특별시 강남구 역삼동", "강남역 11번 출구"));

            ExtractableResponse<Response> extract = searchBy("어린이");

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content.id", Long.class))
                        .containsExactly(target);
            });
        }

        @Test
        void 검색어가_지역에_있으면_조회된다() {
            Long target = communityPostRepository.save(
                    POST_PLACED(user.getId(), "서울특별시 광진구 화양동", "세종대학교")).getId();
            communityPostRepository.save(POST_PLACED(user.getId(), "부산광역시 해운대구 우동", "해운대해수욕장"));

            ExtractableResponse<Response> extract = searchBy("서울");

            assertSoftly(softly -> {
                softly.assertThat(extract.jsonPath().getList("content.id", Long.class))
                        .containsExactly(target);
            });
        }

        @Test
        void 검색어가_장소명에_있으면_조회된다() {
            Long target = communityPostRepository.save(
                    POST_PLACED(user.getId(), "서울특별시 광진구 화양동", "세종대학교")).getId();
            communityPostRepository.save(POST_PLACED(user.getId(), "서울특별시 광진구 화양동", "건대입구역"));

            ExtractableResponse<Response> extract = searchBy("세종대");

            assertSoftly(softly -> {
                softly.assertThat(extract.jsonPath().getList("content.id", Long.class))
                        .containsExactly(target);
            });
        }

        @Test
        void 검색어가_한_글자면_400을_응답한다() {
            ExtractableResponse<Response> extract = searchBy("서");

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 검색어가_비어있으면_전체를_조회한다() {
            communityPostRepository.save(POST(user.getId()));
            communityPostRepository.save(POST(user.getId()));

            ExtractableResponse<Response> extract = searchBy("   ");

            assertSoftly(softly -> {
                softly.assertThat(extract.jsonPath().getList("content")).hasSize(2);
            });
        }

        private ExtractableResponse<Response> searchBy(String keyword) {
            return unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .queryParam("keyword", keyword)
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();
        }

        @Test
        void 거리순은_보낸_좌표에서_가까운_순서로_조회한다() {
            Long seoul = communityPostRepository.save(POST_AT(user.getId(), 37.5665, 126.9780)).getId();
            Long busan = communityPostRepository.save(POST_AT(user.getId(), 35.1796, 129.0756)).getId();
            Long daejeon = communityPostRepository.save(POST_AT(user.getId(), 36.3504, 127.3845)).getId();

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "DISTANCE")
                    .queryParam("latitude", 37.5665)
                    .queryParam("longitude", 126.9780)
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content.id", Long.class))
                        .containsExactly(seoul, daejeon, busan);
            });
        }

        @Test
        void 거리순_커서로_다음_페이지를_중복과_누락_없이_조회한다() {
            for (int i = 0; i < 6; i++) {
                communityPostRepository.save(POST_AT(user.getId(), 37.5 + i * 0.1, 127.0));
            }

            ExtractableResponse<Response> firstExtract = unauthenticated()
                    .queryParam("size", 4)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "DISTANCE")
                    .queryParam("latitude", 37.5)
                    .queryParam("longitude", 127.0)
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();
            String nextCursor = firstExtract.jsonPath().getString("cursor.nextCursor");

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 4)
                    .queryParam("cursor", nextCursor)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "DISTANCE")
                    .queryParam("latitude", 37.5)
                    .queryParam("longitude", 127.0)
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            List<Long> firstIds = firstExtract.jsonPath().getList("content.id", Long.class);
            List<Long> secondIds = extract.jsonPath().getList("content.id", Long.class);
            assertSoftly(softly -> {
                softly.assertThat(firstIds).hasSize(4);
                softly.assertThat(secondIds).hasSize(2);
                softly.assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
            });
        }

        @Test
        void 거리순인데_좌표가_없으면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "DISTANCE")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 거리순이_아닌데_좌표를_보내면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .queryParam("latitude", 37.5665)
                    .queryParam("longitude", 126.9780)
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 인기순은_좋아요_스크랩_채팅참여를_가중합한_점수가_높은_순서로_조회한다() {
            Long low = communityPostRepository.save(POST(user.getId())).getId();
            Long high = communityPostRepository.save(POST(user.getId())).getId();
            Long mid = communityPostRepository.save(POST(user.getId())).getId();

            // high: 채팅 참여 3명 * 3점 = 9점
            for (int i = 0; i < 3; i++) {
                communityChatMemberRepository.save(CommunityChatMember.createMember(high, givenUser("참여자" + i).getId()));
            }
            // mid: 좋아요 1개(1점) + 스크랩 1개(2점) = 3점
            communityPostLikeRepository.save(CommunityPostLike.createLike(mid, givenUser("좋아요러").getId()));
            communityPostScrapRepository.save(CommunityPostScrap.createScrap(mid, givenUser("스크랩러").getId()));
            // low: 반응 없음 = 0점

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "POPULAR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content.id", Long.class))
                        .containsExactly(high, mid, low);
            });
        }

        @Test
        void 인기순은_마감된_글을_점수와_무관하게_맨_뒤로_보낸다() {
            Long completedId = communityPostRepository.save(COMPLETED_POST(user.getId())).getId();
            communityChatMemberRepository.save(
                    CommunityChatMember.createMember(completedId, givenUser("참여자").getId()));
            Long recruitingId = communityPostRepository.save(POST(user.getId())).getId();

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "POPULAR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content.id", Long.class))
                        .containsExactly(recruitingId, completedId);
            });
        }

        @Test
        void 인기순은_작성된지_7일이_넘은_글은_제외한다() {
            Long recent = communityPostRepository.save(POST(user.getId())).getId();
            Long old = communityPostRepository.save(POST(user.getId())).getId();
            for (int i = 0; i < 5; i++) {
                communityChatMemberRepository.save(CommunityChatMember.createMember(old, givenUser("참여자" + i).getId()));
            }
            jdbcTemplate.update(
                    "UPDATE community_posts SET created_at = ? WHERE id = ?",
                    Timestamp.valueOf(LocalDateTime.now().minusDays(8)),
                    old
            );

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "POPULAR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content.id", Long.class))
                        .containsExactly(recent);
            });
        }

        @Test
        void 인기순_커서로_다음_페이지를_중복과_누락_없이_조회한다() {
            for (int i = 0; i < 6; i++) {
                Long postId = communityPostRepository.save(POST(user.getId())).getId();
                for (int j = 0; j <= i; j++) {
                    communityPostLikeRepository.save(CommunityPostLike.createLike(postId, givenUser("유저" + i + "-" + j).getId()));
                }
            }

            ExtractableResponse<Response> firstExtract = unauthenticated()
                    .queryParam("size", 4)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "POPULAR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();
            String nextCursor = firstExtract.jsonPath().getString("cursor.nextCursor");

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 4)
                    .queryParam("cursor", nextCursor)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "POPULAR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            List<Long> firstIds = firstExtract.jsonPath().getList("content.id", Long.class);
            List<Long> secondIds = extract.jsonPath().getList("content.id", Long.class);
            assertSoftly(softly -> {
                softly.assertThat(firstIds).hasSize(4);
                softly.assertThat(secondIds).hasSize(2);
                softly.assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
            });
        }

        @Test
        void 다른_정렬로_받은_커서로_요청하면_400을_응답한다() {
            for (int i = 0; i < 3; i++) {
                communityPostRepository.save(POST(user.getId()));
            }
            String latestCursor = unauthenticated()
                    .queryParam("size", 1)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract()
                    .jsonPath().getString("cursor.nextCursor");

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 1)
                    .queryParam("cursor", latestCursor)
                    .queryParam("countryCode", "KR")
                    .queryParam("sort", "DEADLINE")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        /** 정렬은 쿼리가, status는 자바가 판정한다. 둘이 어긋나면 여기서 깨진다. */
        @Test
        void 목록_순서와_응답_상태가_같은_마감_기준을_따른다() {
            communityPostRepository.save(POST(user.getId()));
            communityPostRepository.save(COMPLETED_POST(user.getId()));
            communityPostRepository.save(PAST_POST(user.getId()));
            communityPostRepository.save(PAST_COMPLETED_POST(user.getId()));

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            List<String> statuses = extract.jsonPath().getList("content.status", String.class);
            int firstClosed = statuses.indexOf(statuses.stream()
                    .filter(status -> !status.equals("RECRUITING"))
                    .findFirst()
                    .orElseThrow());

            assertSoftly(softly -> {
                softly.assertThat(statuses).hasSize(4);
                softly.assertThat(statuses.subList(0, firstClosed)).containsOnly("RECRUITING");
                softly.assertThat(statuses.subList(firstClosed, statuses.size()))
                        .doesNotContain("RECRUITING");
            });
        }

        @Test
        void 마감_경계를_넘는_커서도_중복과_누락_없이_조회한다() {
            for (int i = 0; i < 3; i++) {
                communityPostRepository.save(POST(user.getId()));
                communityPostRepository.save(COMPLETED_POST(user.getId()));
            }

            ExtractableResponse<Response> firstExtract = unauthenticated()
                    .queryParam("size", 4)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();
            String nextCursor = firstExtract.jsonPath().getString("cursor.nextCursor");

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 4)
                    .queryParam("cursor", nextCursor)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            List<Long> firstIds = firstExtract.jsonPath().getList("content.id", Long.class);
            List<Long> secondIds = extract.jsonPath().getList("content.id", Long.class);
            assertSoftly(softly -> {
                softly.assertThat(firstIds).hasSize(4);
                softly.assertThat(secondIds).hasSize(2);
                softly.assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
            });
        }

        @Test
        void 커서로_다음_페이지를_중복과_누락_없이_조회하면_200을_응답한다() {
            for (int i = 0; i < 15; i++) {
                communityPostRepository.save(POST(user.getId()));
            }
            ExtractableResponse<Response> firstExtract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();
            String nextCursor = firstExtract.jsonPath().getString("cursor.nextCursor");

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 10)
                    .queryParam("cursor", nextCursor)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            List<Long> firstIds = firstExtract.jsonPath().getList("content.id", Long.class);
            List<Long> secondIds = extract.jsonPath().getList("content.id", Long.class);
            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(secondIds).hasSize(5);
                softly.assertThat(extract.jsonPath().getBoolean("cursor.hasNext")).isFalse();
                softly.assertThat(extract.jsonPath().getString("cursor.nextCursor")).isNull();
                softly.assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
            });
        }

        @Test
        void 게시글이_없으면_빈_목록으로_200을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content")).isEmpty();
                softly.assertThat(extract.jsonPath().getBoolean("cursor.hasNext")).isFalse();
            });
        }

        @Test
        void 토큰_없이_요청해도_200을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
            });
        }

        @Test
        void 잘못된_커서로_요청하면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("cursor", "broken-cursor!!")
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 지원하지_않는_쿼리_파라미터로_요청하면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("page", 1)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void scope와_sort를_기본값으로_요청하면_200을_응답한다() {
            communityPostRepository.save(POST(user.getId()));

            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("scope", "ALL")
                    .queryParam("sort", "LATEST")
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content")).hasSize(1);
            });
        }

        @Test
        void 아직_지원하지_않는_scope로_요청하면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("scope", "NEARBY")
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
                softly.assertThat(extract.jsonPath().getString("detail"))
                        .isEqualTo(CommunityPostException.UNSUPPORTED_LIST_SCOPE.getDetail());
            });
        }

        @Test
        void 정의되지_않은_scope_값으로_요청하면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("scope", "EVERYTHING")
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void NEARBY용_lat_lng_파라미터로_요청하면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("scope", "NEARBY")
                    .queryParam("lat", 37.4979)
                    .queryParam("lng", 127.0276)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 제거된_page_파라미터로_요청하면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void size가_0이면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 0)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void size가_100을_초과하면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("size", 101)
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }
    }

    @Nested
    @DisplayName("게시글 단건 조회 API")
    class GetOnePost {

        @Test
        void 존재하는_게시글_조회에_성공하면_200을_응답한다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getLong("id")).isEqualTo(postId);
                softly.assertThat(extract.jsonPath().getString("status")).isEqualTo("RECRUITING");
            });
        }

        @Test
        void 모임_날짜가_지난_게시글은_ENDED로_내려온다() {
            Long postId = communityPostRepository.save(PAST_POST(user.getId())).getId();

            ExtractableResponse<Response> extract = unauthenticated()
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("status")).isEqualTo("ENDED");
            });
        }

        @Test
        void 조회_결과에_지번_주소가_함께_내려온다() {
            Long postId = communityPostRepository.save(
                    POST(user.getId(), "서울특별시 광진구 군자동 98")).getId();

            ExtractableResponse<Response> extract = unauthenticated()
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("location.address"))
                        .isEqualTo("서울특별시 광진구 군자동 98");
            });
        }

        @Test
        void 존재하지_않는_게시글_조회시_404를_응답한다() {
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .get(POST_API_URL + "/999")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(404);
            });
        }

        @Test
        void 토큰_없이_요청해도_200을_응답한다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();

            ExtractableResponse<Response> extract = unauthenticated()
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
            });
        }

        @Test
        void 토큰_없이_요청하면_chatJoined는_false다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();

            ExtractableResponse<Response> extract = unauthenticated()
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getBoolean("chatJoined")).isFalse();
            });
        }

        @Test
        void 토큰_없이_요청하면_notificationSettings는_null이다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();

            ExtractableResponse<Response> extract = unauthenticated()
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getMap("notificationSettings")).isNull();
            });
        }

        @Test
        void 내가_쓴_글은_댓글_알림만_켜진_기본값이_내려온다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.jsonPath().getBoolean("notificationSettings.commentNotificationsEnabled")).isTrue();
                softly.assertThat(extract.jsonPath().getBoolean("notificationSettings.replyNotificationsEnabled")).isFalse();
            });
        }

        @Test
        void 남의_글은_알림이_모두_꺼진_기본값이_내려온다() {
            User writer = givenUser("다른작성자");
            Long postId = communityPostRepository.save(POST(writer.getId())).getId();

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.jsonPath().getBoolean("notificationSettings.commentNotificationsEnabled")).isFalse();
                softly.assertThat(extract.jsonPath().getBoolean("notificationSettings.replyNotificationsEnabled")).isFalse();
            });
        }

        @Test
        void 토글을_건드린_글은_저장된_설정이_내려온다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();
            communityPostNotificationSettingRepository.save(
                    CommunityPostNotificationSetting.createSetting(user.getId(), postId, false, true));

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.jsonPath().getBoolean("notificationSettings.commentNotificationsEnabled")).isFalse();
                softly.assertThat(extract.jsonPath().getBoolean("notificationSettings.replyNotificationsEnabled")).isTrue();
            });
        }

        @Test
        void 채팅방에_참여한_로그인_사용자가_조회하면_chatJoined는_true다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();
            communityChatMemberRepository.save(CommunityChatMember.createMember(postId, user.getId()));

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getBoolean("chatJoined")).isTrue();
            });
        }

        @Test
        void 채팅방에_참여하지_않은_로그인_사용자가_조회하면_chatJoined는_false다() {
            User writer = givenUser("작성자");
            Long postId = communityPostRepository.save(POST(writer.getId())).getId();

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .get(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getBoolean("chatJoined")).isFalse();
            });
        }
    }

    @Nested
    @DisplayName("게시글 수정 API")
    class UpdatePost {

        @Test
        void 작성자가_게시글_수정에_성공하면_200을_응답한다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();
            Map<String, Object> location = Map.of("latitude", 37.5665, "longitude", 126.9780, "placeName", "서울시청 광장");
            Map<String, Object> request = Map.of(
                    "title", "수정된 제목",
                    "content", "수정된 내용",
                    "meetingAt", LocalDateTime.now().plusDays(5).toString(),
                    "location", location,
                    "maxParticipants", 8
            );

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .put(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("title")).isEqualTo("수정된 제목");
                softly.assertThat(extract.jsonPath().getString("content")).isEqualTo("수정된 내용");
                softly.assertThat(extract.jsonPath().getInt("maxParticipants")).isEqualTo(8);
            });
        }

        @Test
        void 작성자가_아닌_사용자가_요청하면_403을_응답한다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();
            Map<String, Object> location = Map.of("latitude", 37.5665, "longitude", 126.9780, "placeName", "서울시청 광장");
            Map<String, Object> request = Map.of(
                    "title", "수정된 제목",
                    "content", "수정된 내용",
                    "meetingAt", LocalDateTime.now().plusDays(5).toString(),
                    "location", location,
                    "maxParticipants", 8
            );

            ExtractableResponse<Response> extract = authenticated(otherAccessToken)
                    .body(request)
                    .when()
                    .put(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(403);
            });
        }

        @Test
        void 존재하지_않는_게시글_수정시_404를_응답한다() {
            Map<String, Object> location = Map.of("latitude", 37.5665, "longitude", 126.9780, "placeName", "서울시청 광장");
            Map<String, Object> request = Map.of(
                    "title", "수정된 제목",
                    "content", "수정된 내용",
                    "meetingAt", LocalDateTime.now().plusDays(5).toString(),
                    "location", location,
                    "maxParticipants", 8
            );

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .put(POST_API_URL + "/999")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(404);
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            Map<String, Object> location = Map.of("latitude", 37.5665, "longitude", 126.9780, "placeName", "서울시청 광장");
            Map<String, Object> request = Map.of(
                    "title", "수정된 제목",
                    "content", "수정된 내용",
                    "meetingAt", LocalDateTime.now().plusDays(5).toString(),
                    "location", location,
                    "maxParticipants", 8
            );

            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .put(POST_API_URL + "/1")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }

    @Nested
    @DisplayName("게시글 삭제 API")
    class DeletePost {

        @Test
        void 작성자가_게시글_삭제에_성공하면_204를_응답한다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .delete(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(204);
                softly.assertThat(communityPostRepository.findById(postId)).isEmpty();
            });
        }

        @Test
        void 작성자가_아닌_사용자가_요청하면_403을_응답한다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();

            ExtractableResponse<Response> extract = authenticated(otherAccessToken)
                    .when()
                    .delete(POST_API_URL + "/" + postId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(403);
            });
        }

        @Test
        void 존재하지_않는_게시글_삭제시_404를_응답한다() {
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .delete(POST_API_URL + "/999")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(404);
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .when()
                    .delete(POST_API_URL + "/1")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }

    @Nested
    @DisplayName("모집 상태 변경 API")
    class UpdateStatusApi {

        @Test
        void 작성자가_모집_상태_변경에_성공하면_200을_응답한다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();
            CommunityPostStatusRequest request = new CommunityPostStatusRequest(RecruitmentStatus.COMPLETED);

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .patch(POST_API_URL + "/" + postId + "/status")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("status")).isEqualTo("COMPLETED");
            });
        }

        @Test
        void 작성자가_아닌_사용자가_요청하면_403을_응답한다() {
            Long postId = communityPostRepository.save(POST(user.getId())).getId();
            CommunityPostStatusRequest request = new CommunityPostStatusRequest(RecruitmentStatus.COMPLETED);

            ExtractableResponse<Response> extract = authenticated(otherAccessToken)
                    .body(request)
                    .when()
                    .patch(POST_API_URL + "/" + postId + "/status")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(403);
            });
        }

        @Test
        void 존재하지_않는_게시글_상태_변경시_404를_응답한다() {
            CommunityPostStatusRequest request = new CommunityPostStatusRequest(RecruitmentStatus.COMPLETED);

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .patch(POST_API_URL + "/999/status")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(404);
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            CommunityPostStatusRequest request = new CommunityPostStatusRequest(RecruitmentStatus.COMPLETED);

            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .patch(POST_API_URL + "/1/status")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }
}
