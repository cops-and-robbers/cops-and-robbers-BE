package com.team.cops_and_robbers.bug.presentation;

import com.team.cops_and_robbers.bug.presentation.dto.request.BugReportRequest;
import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.infrastructure.discord.DiscordNotifier;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class BugReportControllerTest extends ControllerTest {

    private static final String BUG_API_URL = "/api/bugs";

    @MockitoBean
    private DiscordNotifier discordNotifier;

    private User user;
    private String accessToken;

    @BeforeEach
    void setUpData() {
        user = givenUser("버그제보자");
        accessToken = givenAccessToken(user);
    }

    @Nested
    @DisplayName("버그 제보 API")
    class CreateBugReport {

        @Test
        void 버그_제보에_성공하면_201을_응답한다() {
            BugReportRequest request = new BugReportRequest("게임 시작 버튼을 누르면 앱이 꺼져요");

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(BUG_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(201);
                softly.assertThat(bugReportRepository.count()).isEqualTo(1);
            });
        }

        @Test
        void content가_blank이면_400을_응답한다() {
            BugReportRequest request = new BugReportRequest("   ");

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(BUG_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void content가_1000자_초과이면_400을_응답한다() {
            BugReportRequest request = new BugReportRequest("a".repeat(1001));

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(BUG_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            BugReportRequest request = new BugReportRequest("게임 시작 버튼을 누르면 앱이 꺼져요");

            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post(BUG_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }
}
