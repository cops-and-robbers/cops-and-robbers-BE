package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.ChatReportFixture;
import com.team.cops_and_robbers.report.domain.ChatReport;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class AdminReportResolverTest extends ControllerTest {

    private static final String GRAPHQL_URL = "/graphql";

    private String adminToken;

    @BeforeEach
    void setUpAdmin() {
        adminToken = givenAccessToken(givenAdminUser("admin"));
    }

    @Nested
    @DisplayName("인증 검사")
    class Authentication {

        @Test
        void 토큰_없이_요청하면_401을_반환한다() {
            ExtractableResponse<Response> response = executeQuery(
                    "{ adminReports { totalElements content { id } } }");

            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        void 일반_유저_토큰으로_요청하면_403을_반환한다() {
            User user = givenUser("normalUser");
            String userToken = givenAccessToken(user);

            ExtractableResponse<Response> response = executeQuery(
                    userToken, "{ adminReports { totalElements content { id } } }");

            assertThat(response.statusCode()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("신고 목록 조회")
    class AdminReports {

        @Test
        void 신고_목록을_페이지로_조회한다() {
            User reporter = givenUser("reporter");
            User reported = givenUser("reported");
            reportRepository.save(ChatReportFixture.VERBAL_ABUSE_REPORT(10L, reporter.getId(), reported.getId()));

            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminReports(page: 0, size: 10) { totalElements totalPages page size content { id reporterNickname reportedNickname status reportType } } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminReports.totalElements")).isGreaterThanOrEqualTo(1);
                softly.assertThat(response.jsonPath().getString("data.adminReports.content[0].reporterNickname")).isEqualTo("reporter");
                softly.assertThat(response.jsonPath().getString("data.adminReports.content[0].reportedNickname")).isEqualTo("reported");
                softly.assertThat(response.jsonPath().getString("data.adminReports.content[0].status")).isEqualTo("PENDING");
            });
        }

        @Test
        void 상태로_신고를_필터링한다() {
            User reporter = givenUser("reporter");
            User reported = givenUser("reported");
            reportRepository.save(ChatReportFixture.VERBAL_ABUSE_REPORT(10L, reporter.getId(), reported.getId()));

            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminReports(status: PENDING) { totalElements content { id status } } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminReports.totalElements")).isGreaterThanOrEqualTo(1);
                softly.assertThat(response.jsonPath().getString("data.adminReports.content[0].status")).isEqualTo("PENDING");
            });
        }

        @Test
        void 결과가_없으면_빈_목록을_반환한다() {
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminReports(status: RESOLVED) { totalElements content { id } } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminReports.totalElements")).isZero();
                softly.assertThat(response.jsonPath().getList("data.adminReports.content")).isEmpty();
            });
        }

        @Test
        void 탈퇴한_유저의_닉네임은_알수없음으로_반환된다() {
            reportRepository.save(ChatReportFixture.VERBAL_ABUSE_REPORT(10L, 9999L, 8888L));

            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminReports { totalElements content { id reporterNickname reportedNickname } } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.adminReports.content[0].reporterNickname")).isEqualTo("알수없음");
                softly.assertThat(response.jsonPath().getString("data.adminReports.content[0].reportedNickname")).isEqualTo("알수없음");
            });
        }
    }

    @Nested
    @DisplayName("신고 상태 변경")
    class UpdateReportStatus {

        @Test
        void 신고_상태를_변경하면_변경된_정보를_반환한다() {
            User reporter = givenUser("reporter");
            User reported = givenUser("reported");
            ChatReport report = reportRepository.save(
                    ChatReportFixture.VERBAL_ABUSE_REPORT(10L, reporter.getId(), reported.getId()));

            ExtractableResponse<Response> response = executeMutation(
                    adminToken,
                    "mutation { updateReportStatus(reportId: " + report.getId() + ", status: RESOLVED, adminMemo: \"처리 완료\") { id status adminMemo reporterNickname reportedNickname } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.updateReportStatus.status")).isEqualTo("RESOLVED");
                softly.assertThat(response.jsonPath().getString("data.updateReportStatus.adminMemo")).isEqualTo("처리 완료");
                softly.assertThat(response.jsonPath().getString("data.updateReportStatus.reporterNickname")).isEqualTo("reporter");
            });
        }

        @Test
        void 탈퇴한_유저의_신고도_상태를_변경할_수_있다() {
            ChatReport report = reportRepository.save(
                    ChatReportFixture.VERBAL_ABUSE_REPORT(10L, 9999L, 8888L));

            ExtractableResponse<Response> response = executeMutation(
                    adminToken,
                    "mutation { updateReportStatus(reportId: " + report.getId() + ", status: DISMISSED) { id status reporterNickname reportedNickname } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.updateReportStatus.status")).isEqualTo("DISMISSED");
                softly.assertThat(response.jsonPath().getString("data.updateReportStatus.reporterNickname")).isEqualTo("알수없음");
                softly.assertThat(response.jsonPath().getString("data.updateReportStatus.reportedNickname")).isEqualTo("알수없음");
            });
        }
    }

    private ExtractableResponse<Response> executeQuery(String token, String query) {
        return authenticated(token)
                .body("{\"query\": \"" + query.replace("\"", "\\\"").replace("\n", " ") + "\"}")
                .when()
                .post(GRAPHQL_URL)
                .then()
                .extract();
    }

    private ExtractableResponse<Response> executeQuery(String query) {
        return unauthenticated()
                .body("{\"query\": \"" + query.replace("\"", "\\\"").replace("\n", " ") + "\"}")
                .when()
                .post(GRAPHQL_URL)
                .then()
                .extract();
    }

    private ExtractableResponse<Response> executeMutation(String token, String mutation) {
        return authenticated(token)
                .body("{\"query\": \"" + mutation.replace("\"", "\\\"").replace("\n", " ") + "\"}")
                .when()
                .post(GRAPHQL_URL)
                .then()
                .extract();
    }
}
