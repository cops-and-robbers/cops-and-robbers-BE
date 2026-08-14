package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostStatusRequest;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class CommunityPostControllerTest extends ControllerTest {

    private static final String POST_API_URL = "/api/community-posts";

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
    @DisplayName("게시글 생성 API")
    class CreatePost {

        @Test
        void 사용자가_게시글_생성에_성공하면_201을_응답한다() {
            Map<String, Object> location = Map.of("latitude", 37.4979, "longitude", 127.0276);
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
        void 토큰_없이_요청하면_401을_응답한다() {
            Map<String, Object> location = Map.of("latitude", 37.4979, "longitude", 127.0276);
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
        void 게시글_목록_조회에_성공하면_200을_응답한다() {
            communityPostRepository.save(POST(user.getId()));
            communityPostRepository.save(POST(user.getId()));

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content")).hasSize(2);
                softly.assertThat(extract.jsonPath().getLong("page.totalElements")).isEqualTo(2);
                softly.assertThat(extract.jsonPath().getInt("page.totalPages")).isEqualTo(1);
            });
        }

        @Test
        void 음수_페이지를_요청하면_400을_응답한다() {
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .queryParam("page", -1)
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
            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .queryParam("size", 0)
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 토큰_없이_요청해도_200을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .when()
                    .get(POST_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
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
            Map<String, Object> location = Map.of("latitude", 37.5665, "longitude", 126.9780);
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
            Map<String, Object> location = Map.of("latitude", 37.5665, "longitude", 126.9780);
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
            Map<String, Object> location = Map.of("latitude", 37.5665, "longitude", 126.9780);
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
            Map<String, Object> location = Map.of("latitude", 37.5665, "longitude", 126.9780);
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
