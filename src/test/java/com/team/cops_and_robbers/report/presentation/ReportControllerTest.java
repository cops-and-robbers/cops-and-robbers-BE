package com.team.cops_and_robbers.report.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.report.domain.ReportType;
import com.team.cops_and_robbers.report.presentation.dto.request.ReportRequest;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ReportControllerTest extends ControllerTest {

    private static final String REPORT_API_URL = "/api/report/chat";

    private User reporter;
    private User reported;
    private Game game;
    private GameParticipant reportedParticipant;
    private String accessToken;

    @BeforeEach
    void setUpData() {
        reporter = givenUser("신고자");
        reported = givenUser("신고대상");
        accessToken = givenAccessToken(reporter);
        game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        givenHost(game, reporter);
        reportedParticipant = givenGuest(game, reported);
    }

    @Nested
    @DisplayName("채팅 신고 API")
    class ChatReport {

        @Test
        void 신고에_성공하면_201을_응답한다() {
            ReportRequest request = new ReportRequest(game.getId(), reportedParticipant.getId(), "나쁜말 했어요", ReportType.VERBAL_ABUSE, null);

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(REPORT_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(201);
                softly.assertThat(reportRepository.count()).isEqualTo(1);
            });
        }

        @Test
        void 같은_게임에서_동일_유저_중복_신고시_409를_응답한다() {
            ReportRequest request = new ReportRequest(game.getId(), reportedParticipant.getId(), "나쁜말 했어요", ReportType.VERBAL_ABUSE, null);

            authenticated(accessToken).body(request).when().post("/api/report/chat");

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(REPORT_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(409);
            });
        }

        @Test
        void 본인을_신고하면_400을_응답한다() {
            GameParticipant reporterParticipant = gameParticipantRepository.findByGameIdAndUserId(game.getId(), reporter.getId()).get();
            ReportRequest request = new ReportRequest(game.getId(), reporterParticipant.getId(), "나쁜말 했어요", ReportType.VERBAL_ABUSE, null);

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(REPORT_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 게임이_진행중이_아니면_400을_응답한다() {
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            User anotherUser = givenUser("다른유저");
            givenGuest(waitingGame, reporter);
            GameParticipant target = givenGuest(waitingGame, anotherUser);
            ReportRequest request = new ReportRequest(waitingGame.getId(), target.getId(), "나쁜말 했어요", ReportType.VERBAL_ABUSE, null);

            ExtractableResponse<Response> extract = authenticated(accessToken)
                    .body(request)
                    .when()
                    .post(REPORT_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(400);
            });
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            ReportRequest request = new ReportRequest(game.getId(), reportedParticipant.getId(), "나쁜말 했어요", ReportType.VERBAL_ABUSE, null);

            ExtractableResponse<Response> extract = unauthenticated()
                    .body(request)
                    .when()
                    .post(REPORT_API_URL)
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(401);
            });
        }
    }
}
