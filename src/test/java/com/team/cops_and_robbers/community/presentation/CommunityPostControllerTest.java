package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

class CommunityPostControllerTest extends ControllerTest {

    private static final String POST_API_URL = "/api/community-posts";
    private static final String ADDRESS_API_URL = POST_API_URL + "/address";
    private static final String COUNTRY_API_URL = POST_API_URL + "/country";

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
                softly.assertThat(extract.jsonPath().getString("content[0].location.region")).isNull();
                softly.assertThat(extract.jsonPath().getString("content[0].location.placeName"))
                        .isEqualTo("어린이대공원 정문");
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
                    .queryParam("keyword", "강남")
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
        void 아직_지원하지_않는_sort로_요청하면_400을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .queryParam("sort", "POPULAR")
                    .queryParam("countryCode", "KR")
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
                softly.assertThat(extract.jsonPath().getString("detail"))
                        .isEqualTo(CommunityPostException.UNSUPPORTED_LIST_SORT.getDetail());
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
