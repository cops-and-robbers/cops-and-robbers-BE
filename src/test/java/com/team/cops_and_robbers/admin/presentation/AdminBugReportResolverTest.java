package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.bug.domain.BugReport;
import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.BugReportFixture;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class AdminBugReportResolverTest extends ControllerTest {

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
                    "{ adminBugReports { totalElements content { id } } }");

            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        void 일반_유저_토큰으로_요청하면_403을_반환한다() {
            User user = givenUser("normalUser");
            String userToken = givenAccessToken(user);

            ExtractableResponse<Response> response = executeQuery(
                    userToken, "{ adminBugReports { totalElements content { id } } }");

            assertThat(response.statusCode()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("버그 제보 목록 조회")
    class AdminBugReports {

        @Test
        void 버그_제보_목록을_페이지로_조회한다() {
            User user = givenUser("reporter");
            bugReportRepository.save(BugReportFixture.PENDING_BUG_REPORT(user.getId()));

            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminBugReports(page: 0, size: 10) { totalElements totalPages page size content { id content userNickname status } } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminBugReports.totalElements")).isGreaterThanOrEqualTo(1);
                softly.assertThat(response.jsonPath().getString("data.adminBugReports.content[0].userNickname")).isEqualTo("reporter");
                softly.assertThat(response.jsonPath().getString("data.adminBugReports.content[0].status")).isEqualTo("PENDING");
            });
        }

        @Test
        void 상태로_버그_제보를_필터링한다() {
            User user = givenUser("reporter");
            bugReportRepository.save(BugReportFixture.PENDING_BUG_REPORT(user.getId()));

            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminBugReports(status: PENDING) { totalElements content { id status } } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminBugReports.totalElements")).isGreaterThanOrEqualTo(1);
                softly.assertThat(response.jsonPath().getString("data.adminBugReports.content[0].status")).isEqualTo("PENDING");
            });
        }

        @Test
        void 결과가_없으면_빈_목록을_반환한다() {
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminBugReports(status: RESOLVED) { totalElements content { id } } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminBugReports.totalElements")).isZero();
                softly.assertThat(response.jsonPath().getList("data.adminBugReports.content")).isEmpty();
            });
        }

        @Test
        void 탈퇴한_유저의_닉네임은_알수없음으로_반환된다() {
            bugReportRepository.save(BugReportFixture.PENDING_BUG_REPORT(9999L));

            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminBugReports { totalElements content { id userNickname } } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.adminBugReports.content[0].userNickname")).isEqualTo("알수없음");
            });
        }
    }

    @Nested
    @DisplayName("버그 제보 상태 변경")
    class UpdateBugReportStatus {

        @Test
        void 버그_제보_상태를_변경하면_변경된_정보를_반환한다() {
            User user = givenUser("reporter");
            BugReport bugReport = bugReportRepository.save(BugReportFixture.PENDING_BUG_REPORT(user.getId()));

            ExtractableResponse<Response> response = executeMutation(
                    adminToken,
                    "mutation { updateBugReportStatus(bugReportId: " + bugReport.getId() + ", status: RESOLVED, adminMemo: \"처리 완료\") { id status adminMemo userNickname } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.updateBugReportStatus.status")).isEqualTo("RESOLVED");
                softly.assertThat(response.jsonPath().getString("data.updateBugReportStatus.adminMemo")).isEqualTo("처리 완료");
                softly.assertThat(response.jsonPath().getString("data.updateBugReportStatus.userNickname")).isEqualTo("reporter");
            });
        }

        @Test
        void 탈퇴한_유저의_버그_제보도_상태를_변경할_수_있다() {
            BugReport bugReport = bugReportRepository.save(BugReportFixture.PENDING_BUG_REPORT(9999L));

            ExtractableResponse<Response> response = executeMutation(
                    adminToken,
                    "mutation { updateBugReportStatus(bugReportId: " + bugReport.getId() + ", status: RESOLVED) { id status userNickname } }");

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.updateBugReportStatus.status")).isEqualTo("RESOLVED");
                softly.assertThat(response.jsonPath().getString("data.updateBugReportStatus.userNickname")).isEqualTo("알수없음");
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
