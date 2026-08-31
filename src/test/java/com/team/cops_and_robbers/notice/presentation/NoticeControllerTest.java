package com.team.cops_and_robbers.notice.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeCreateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeTranslationRequest;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeUpdateRequest;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.team.cops_and_robbers.common.fixture.NoticeFixture.JA_TRANSLATION;
import static com.team.cops_and_robbers.common.fixture.NoticeFixture.KO_TRANSLATION;
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

    private static NoticeTranslationRequest koTranslation() {
        return new NoticeTranslationRequest("ko", "공지사항 제목", "공지사항 내용");
    }

    private static NoticeTranslationRequest jaTranslation() {
        return new NoticeTranslationRequest("ja", "お知らせのタイトル", "お知らせの内容");
    }

    /** 한국어 번역만 가진 공지 하나를 저장하고 id를 돌려준다. */
    private Long givenKoreanNotice() {
        Long noticeId = noticeRepository.save(NOTICE()).getId();
        noticeTranslationRepository.save(KO_TRANSLATION(noticeId));
        return noticeId;
    }

    @Nested
    @DisplayName("공지사항 생성 API")
    class CreateNotice {

        @Test
        void 관리자가_번역과_함께_공지사항_생성에_성공하면_201을_응답한다() {
            NoticeCreateRequest request = new NoticeCreateRequest(
                    false, NoticeCategory.NOTICE, "ko", List.of(koTranslation(), jaTranslation()));

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
                softly.assertThat(extract.jsonPath().getString("language")).isEqualTo("ko");
                softly.assertThat(extract.jsonPath().getString("requestedLanguage")).isEqualTo("ko");
                softly.assertThat(extract.jsonPath().getBoolean("pinned")).isFalse();
                softly.assertThat(extract.jsonPath().getString("category")).isEqualTo("NOTICE");
            });
        }

        @Test
        void 원문_언어의_번역이_없으면_400을_응답한다() {
            NoticeCreateRequest request = new NoticeCreateRequest(
                    false, NoticeCategory.NOTICE, "ko", List.of(jaTranslation()));

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(request)
                    .when()
                    .post(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 같은_언어의_번역이_두_번_오면_400을_응답한다() {
            NoticeCreateRequest request = new NoticeCreateRequest(
                    false, NoticeCategory.NOTICE, "ko", List.of(koTranslation(), koTranslation()));

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(request)
                    .when()
                    .post(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 지원하지_않는_언어로_요청하면_400을_응답한다() {
            NoticeCreateRequest request = new NoticeCreateRequest(
                    false, NoticeCategory.NOTICE, "fr",
                    List.of(new NoticeTranslationRequest("fr", "제목", "내용")));

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(request)
                    .when()
                    .post(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 일반_사용자가_요청하면_403을_응답한다() {
            NoticeCreateRequest request = new NoticeCreateRequest(
                    false, NoticeCategory.NOTICE, "ko", List.of(koTranslation()));

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
            NoticeCreateRequest request = new NoticeCreateRequest(
                    false, NoticeCategory.NOTICE, "ko", List.of(koTranslation()));

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
            givenKoreanNotice();
            Long pinnedId = noticeRepository.save(PINNED_NOTICE()).getId();
            noticeTranslationRepository.save(KO_TRANSLATION(pinnedId));

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
        void 요청한_언어의_번역이_있으면_그_언어로_내려간다() {
            Long noticeId = givenKoreanNotice();
            noticeTranslationRepository.save(JA_TRANSLATION(noticeId));

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .queryParam("language", "ja")
                    .when()
                    .get(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("content[0].title")).isEqualTo("お知らせのタイトル");
                softly.assertThat(extract.jsonPath().getString("content[0].language")).isEqualTo("ja");
                softly.assertThat(extract.jsonPath().getString("content[0].requestedLanguage")).isEqualTo("ja");
            });
        }

        @Test
        void 요청한_언어의_번역이_없으면_원문_언어로_대체된다() {
            givenKoreanNotice();

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .queryParam("language", "ja")
                    .when()
                    .get(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("content[0].title")).isEqualTo("공지사항 제목");
                softly.assertThat(extract.jsonPath().getString("content[0].language")).isEqualTo("ko");
                softly.assertThat(extract.jsonPath().getString("content[0].requestedLanguage")).isEqualTo("ja");
            });
        }

        @Test
        void language를_보내지_않으면_한국어로_내려간다() {
            givenKoreanNotice();

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .when()
                    .get(NOTICE_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("content[0].title")).isEqualTo("공지사항 제목");
                softly.assertThat(extract.jsonPath().getString("content[0].language")).isEqualTo("ko");
                softly.assertThat(extract.jsonPath().getString("content[0].requestedLanguage")).isEqualTo("ko");
            });
        }

        @Test
        void category_필터로_조회하면_해당_카테고리만_반환된다() {
            givenKoreanNotice();        // NOTICE 카테고리
            Long pinnedId = noticeRepository.save(PINNED_NOTICE()).getId();
            noticeTranslationRepository.save(KO_TRANSLATION(pinnedId));

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
            givenKoreanNotice();

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
            Long noticeId = givenKoreanNotice();

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .when()
                    .get(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getLong("id")).isEqualTo(noticeId);
                softly.assertThat(extract.jsonPath().getString("category")).isEqualTo("NOTICE");
                softly.assertThat(extract.jsonPath().getString("language")).isEqualTo("ko");
            });
        }

        @Test
        void 요청한_언어의_번역이_없으면_원문_언어로_대체된다() {
            Long noticeId = givenKoreanNotice();

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .queryParam("language", "ja")
                    .when()
                    .get(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getString("title")).isEqualTo("공지사항 제목");
                softly.assertThat(extract.jsonPath().getString("language")).isEqualTo("ko");
                softly.assertThat(extract.jsonPath().getString("requestedLanguage")).isEqualTo("ja");
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
    @DisplayName("공지사항 번역 전체 조회 API")
    class GetNoticeTranslations {

        @Test
        void 관리자가_조회하면_저장된_번역_전체를_200으로_응답한다() {
            Long noticeId = givenKoreanNotice();
            noticeTranslationRepository.save(JA_TRANSLATION(noticeId));

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .when()
                    .get(NOTICE_API_URL + "/" + noticeId + "/translations")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.jsonPath().getLong("noticeId")).isEqualTo(noticeId);
                softly.assertThat(extract.jsonPath().getString("originalLanguage")).isEqualTo("ko");
                softly.assertThat(extract.jsonPath().getList("translations")).hasSize(2);
            });
        }

        @Test
        void 일반_사용자가_요청하면_403을_응답한다() {
            Long noticeId = givenKoreanNotice();

            ExtractableResponse<Response> extract = authenticated(userAccessToken)
                    .when()
                    .get(NOTICE_API_URL + "/" + noticeId + "/translations")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(403);
            });
        }

        @Test
        void 존재하지_않는_공지사항이면_404를_응답한다() {
            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .when()
                    .get(NOTICE_API_URL + "/999/translations")
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
                    .get(NOTICE_API_URL + "/1/translations")
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
        void 관리자가_번역을_교체하며_수정에_성공하면_200을_응답한다() {
            Long noticeId = givenKoreanNotice();
            NoticeUpdateRequest request = new NoticeUpdateRequest(
                    true, NoticeCategory.MAINTENANCE, "ko",
                    List.of(new NoticeTranslationRequest("ko", "수정된 제목", "수정된 내용"), jaTranslation()));

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
        void 원문_언어의_번역이_없으면_400을_응답한다() {
            Long noticeId = givenKoreanNotice();
            NoticeUpdateRequest request = new NoticeUpdateRequest(
                    true, NoticeCategory.NOTICE, "ko", List.of(jaTranslation()));

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .body(request)
                    .when()
                    .put(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 일반_사용자가_요청하면_403을_응답한다() {
            Long noticeId = givenKoreanNotice();
            NoticeUpdateRequest request = new NoticeUpdateRequest(
                    true, NoticeCategory.NOTICE, "ko", List.of(koTranslation()));

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
            NoticeUpdateRequest request = new NoticeUpdateRequest(
                    true, NoticeCategory.NOTICE, "ko", List.of(koTranslation()));

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
            NoticeUpdateRequest request = new NoticeUpdateRequest(
                    true, NoticeCategory.NOTICE, "ko", List.of(koTranslation()));

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
    @DisplayName("공지사항 삭제 API")
    class DeleteNotice {

        @Test
        void 관리자가_공지사항_삭제에_성공하면_204를_응답하고_번역도_함께_삭제된다() {
            Long noticeId = givenKoreanNotice();

            ExtractableResponse<Response> extract = authenticated(adminAccessToken)
                    .when()
                    .delete(NOTICE_API_URL + "/" + noticeId)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(204);
                softly.assertThat(noticeRepository.findById(noticeId)).isEmpty();
                softly.assertThat(noticeTranslationRepository.findAllByNoticeId(noticeId)).isEmpty();
            });
        }

        @Test
        void 일반_사용자가_요청하면_403을_응답한다() {
            Long noticeId = givenKoreanNotice();

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
