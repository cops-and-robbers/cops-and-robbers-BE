package com.team.cops_and_robbers.notice.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeCreateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeUpdateRequest;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.team.cops_and_robbers.common.fixture.NoticeFixture.NOTICE;
import static com.team.cops_and_robbers.common.fixture.NoticeFixture.PINNED_NOTICE;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class NoticeControllerTest extends ControllerTest {

    private static final String NOTICE_API_URL = "/api/notices";

    private User adminUser;
    private String adminAccessToken;
    private String userAccessToken;

    @BeforeEach
    void setUpData() {
        adminUser = givenAdminUser();
        adminAccessToken = givenAccessToken(adminUser);
        userAccessToken = givenAccessToken(givenUser("일반유저"));
    }

    @Nested
    @DisplayName("공지사항 생성 API")
    class CreateNotice {

        @Test
        void 관리자가_공지사항_생성에_성공하면_201을_응답한다() {
            NoticeCreateRequest request = new NoticeCreateRequest("공지사항 제목", "공지사항 내용", false, NoticeCategory.NOTICE);

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(request)
                    .when()
                    .post(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(201);
                softly.assertThat(extract.jsonPath().getLong("id")).isNotNull();
                softly.assertThat(extract.jsonPath().getString("title")).isEqualTo("공지사항 제목");
                softly.assertThat(extract.jsonPath().getString("content")).isEqualTo("공지사항 내용");
                softly.assertThat(extract.jsonPath().getBoolean("pinned")).isFalse();
                softly.assertThat(extract.jsonPath().getString("category")).isEqualTo("NOTICE");
            });
        }

        @Test
        void 일반_사용자가_요청하면_403을_응답한다() {
            NoticeCreateRequest request = new NoticeCreateRequest("공지사항 제목", "공지사항 내용", false, NoticeCategory.NOTICE);

            ExtractableResponse<Response> extract = authenticated(userAccessToken)
                    .body(request)
                    .when()
                    .post(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(403);
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            NoticeCreateRequest request = new NoticeCreateRequest("공지사항 제목", "공지사항 내용", false, NoticeCategory.NOTICE);

            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }

    @Nested
    @DisplayName("공지사항 목록 조회 API")
    class GetAllNotices {

        @Test
        void 공지사항_목록_조회에_성공하면_200을_응답한다() {
            noticeRepository.save(NOTICE());
            noticeRepository.save(PINNED_NOTICE());

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .when()
                    .get(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content")).hasSize(2);
                softly.assertThat(extract.jsonPath().getBoolean("content[0].pinned")).isTrue();
                softly.assertThat(extract.jsonPath().getBoolean("content[1].pinned")).isFalse();
                softly.assertThat(extract.jsonPath().getLong("page.totalElements")).isEqualTo(2);
                softly.assertThat(extract.jsonPath().getInt("page.totalPages")).isEqualTo(1);
            });
        }

        @Test
        void category_필터로_조회하면_해당_카테고리만_반환된다() {
            noticeRepository.save(NOTICE());        // NOTICE 카테고리
            noticeRepository.save(PINNED_NOTICE());

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .queryParam("category", "NOTICE")
                    .when()
                    .get(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content")).hasSize(2);
                softly.assertThat(extract.jsonPath().getString("content[0].category")).isEqualTo("NOTICE");
                softly.assertThat(extract.jsonPath().getString("content[1].category")).isEqualTo("NOTICE");
            });
        }

        @Test
        void 존재하지_않는_category로_조회하면_빈_목록을_반환한다() {
            noticeRepository.save(NOTICE());

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .queryParam("category", "EVENT")
                    .when()
                    .get(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("content")).isEmpty();
            });
        }

        @Test
        void 음수_페이지를_요청하면_400을_응답한다() {
            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .queryParam("page", -1)
                    .when()
                    .get(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void size가_0이면_400을_응답한다() {
            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .queryParam("size", 0)
                    .when()
                    .get(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            ExtractableResponse<Response> extract = unauthenticated()
                    .when()
                    .get(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }

    @Nested
    @DisplayName("공지사항 단건 조회 API")
    class GetOneNotice {

        @Test
        void 존재하는_공지사항_조회에_성공하면_200을_응답한다() {
            Long noticeId = noticeRepository.save(NOTICE()).getId();

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .when()
                    .get(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getLong("id")).isEqualTo(noticeId);
                softly.assertThat(extract.jsonPath().getString("category")).isEqualTo("NOTICE");
            });
        }

        @Test
        void 존재하지_않는_공지사항_조회시_404를_응답한다() {
            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .when()
                    .get(NOTICE_API_URL + "/999")
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
                    .get(NOTICE_API_URL + "/1")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }

    @Nested
    @DisplayName("공지사항 수정 API")
    class UpdateNotice {

        @Test
        void 관리자가_공지사항_수정에_성공하면_200을_응답한다() {
            Long noticeId = noticeRepository.save(NOTICE()).getId();
            NoticeUpdateRequest request = new NoticeUpdateRequest("수정된 제목", "수정된 내용", true, NoticeCategory.MAINTENANCE);

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(request)
                    .when()
                    .put(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("title")).isEqualTo("수정된 제목");
                softly.assertThat(extract.jsonPath().getString("content")).isEqualTo("수정된 내용");
                softly.assertThat(extract.jsonPath().getBoolean("pinned")).isTrue();
                softly.assertThat(extract.jsonPath().getString("category")).isEqualTo("MAINTENANCE");
            });
        }

        @Test
        void 일반_사용자가_요청하면_403을_응답한다() {
            Long noticeId = noticeRepository.save(NOTICE()).getId();
            NoticeUpdateRequest request = new NoticeUpdateRequest("수정된 제목", "수정된 내용", true, NoticeCategory.NOTICE);

            ExtractableResponse<Response> extract = authenticated(userAccessToken)
                    .body(request)
                    .when()
                    .put(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(403);
            });
        }

        @Test
        void 존재하지_않는_공지사항_수정시_404를_응답한다() {
            NoticeUpdateRequest request = new NoticeUpdateRequest("수정된 제목", "수정된 내용", true, NoticeCategory.NOTICE);

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(request)
                    .when()
                    .put(NOTICE_API_URL + "/999")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(404);
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            NoticeUpdateRequest request = new NoticeUpdateRequest("수정된 제목", "수정된 내용", true, NoticeCategory.NOTICE);

            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .put(NOTICE_API_URL + "/1")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }

    @Nested
    @DisplayName("카테고리 하위 호환성")
    class CategoryBackwardCompatibility {

        @Test
        void 생성_요청에_category가_null이면_NOTICE로_기본_설정된다() {
            NoticeCreateRequest request = new NoticeCreateRequest("공지사항 제목", "공지사항 내용", false, null);

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(request)
                    .when()
                    .post(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(201);
                softly.assertThat(extract.jsonPath().getString("category")).isEqualTo("NOTICE");
            });
        }

        @Test
        void 생성_요청에_category_필드_자체가_없으면_NOTICE로_기본_설정된다() {
            Map<String, Object> body = Map.of(
                    "title", "공지사항 제목",
                    "content", "공지사항 내용",
                    "pinned", false
            );

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(body)
                    .when()
                    .post(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(201);
                softly.assertThat(extract.jsonPath().getString("category")).isEqualTo("NOTICE");
            });
        }

        @Test
        void 수정_요청에_category가_null이면_NOTICE로_기본_설정된다() {
            Long noticeId = noticeRepository.save(NOTICE()).getId();
            NoticeUpdateRequest request = new NoticeUpdateRequest("수정된 제목", "수정된 내용", true, null);

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(request)
                    .when()
                    .put(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("category")).isEqualTo("NOTICE");
            });
        }

        @Test
        void 수정_요청에_category_필드_자체가_없으면_NOTICE로_기본_설정된다() {
            Long noticeId = noticeRepository.save(NOTICE()).getId();
            Map<String, Object> body = Map.of(
                    "title", "수정된 제목",
                    "content", "수정된 내용",
                    "pinned", true
            );

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(body)
                    .when()
                    .put(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("category")).isEqualTo("NOTICE");
            });
        }
    }

    @Nested
    @DisplayName("공지사항 삭제 API")
    class DeleteNotice {

        @Test
        void 관리자가_공지사항_삭제에_성공하면_204를_응답한다() {
            Long noticeId = noticeRepository.save(NOTICE()).getId();

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .when()
                    .delete(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(204);
                softly.assertThat(noticeRepository.findById(noticeId)).isEmpty();
            });
        }

        @Test
        void 일반_사용자가_요청하면_403을_응답한다() {
            Long noticeId = noticeRepository.save(NOTICE()).getId();

            ExtractableResponse<Response> extract = authenticated(userAccessToken)
                    .when()
                    .delete(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(403);
            });
        }

        @Test
        void 존재하지_않는_공지사항_삭제시_404를_응답한다() {
            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .when()
                    .delete(NOTICE_API_URL + "/999")
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
                    .delete(NOTICE_API_URL + "/1")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }
}
