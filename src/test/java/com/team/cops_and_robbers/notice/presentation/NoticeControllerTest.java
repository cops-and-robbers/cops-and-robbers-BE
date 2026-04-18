package com.team.cops_and_robbers.notice.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeCreateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeUpdateRequest;
import com.team.cops_and_robbers.notice.repository.NoticeRepository;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.team.cops_and_robbers.common.fixture.NoticeFixture.NOTICE;
import static com.team.cops_and_robbers.common.fixture.NoticeFixture.PINNED_NOTICE;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class NoticeControllerTest extends ControllerTest {

    private static final String NOTICE_API_URL = "/api/notices";

    @Autowired
    private NoticeRepository noticeRepository;

    private User user;
    private String accessToken;

    @BeforeEach
    void setUpData() {
        user = givenUser();
        accessToken = givenAccessToken(user);
    }

    @Nested
    @DisplayName("공지사항 생성 API")
    class CreateNotice {

        @Test
        void 공지사항_생성에_성공하면_201을_응답한다() {
            NoticeCreateRequest request = new NoticeCreateRequest("공지사항 제목", "공지사항 내용", false);

            ExtractableResponse<Response> extract = authenticated(accessToken)
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
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            NoticeCreateRequest request = new NoticeCreateRequest("공지사항 제목", "공지사항 내용", false);

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

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .get(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getList("")).hasSize(2);
                softly.assertThat(extract.jsonPath().getBoolean("[0].pinned")).isTrue();
                softly.assertThat(extract.jsonPath().getBoolean("[1].pinned")).isFalse();
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

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .when()
                    .get(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getLong("id")).isEqualTo(noticeId);
            });
        }

        @Test
        void 존재하지_않는_공지사항_조회시_404를_응답한다() {
            ExtractableResponse<Response> extract = authenticated(accessToken)
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
        void 공지사항_수정에_성공하면_200을_응답한다() {
            Long noticeId = noticeRepository.save(NOTICE()).getId();
            NoticeUpdateRequest request = new NoticeUpdateRequest("수정된 제목", "수정된 내용", true);

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .patch(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("title")).isEqualTo("수정된 제목");
                softly.assertThat(extract.jsonPath().getString("content")).isEqualTo("수정된 내용");
                softly.assertThat(extract.jsonPath().getBoolean("pinned")).isTrue();
            });
        }

        @Test
        void 존재하지_않는_공지사항_수정시_404를_응답한다() {
            NoticeUpdateRequest request = new NoticeUpdateRequest("수정된 제목", "수정된 내용", true);

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .patch(NOTICE_API_URL + "/999")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(404);
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            NoticeUpdateRequest request = new NoticeUpdateRequest("수정된 제목", "수정된 내용", true);

            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .patch(NOTICE_API_URL + "/1")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }

    @Nested
    @DisplayName("공지사항 삭제 API")
    class DeleteNotice {

        @Test
        void 공지사항_삭제에_성공하면_204를_응답한다() {
            Long noticeId = noticeRepository.save(NOTICE()).getId();

            ExtractableResponse<Response> extract = authenticated(accessToken)
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
        void 존재하지_않는_공지사항_삭제시_404를_응답한다() {
            ExtractableResponse<Response> extract = authenticated(accessToken)
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
