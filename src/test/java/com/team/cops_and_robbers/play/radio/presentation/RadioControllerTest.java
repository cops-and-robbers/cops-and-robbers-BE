package com.team.cops_and_robbers.play.radio.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.play.radio.presentation.dto.response.RadioTokenResponse;
import com.team.cops_and_robbers.play.radio.repository.RadioOverloadRepository;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class RadioControllerTest extends ControllerTest {

    private static final String GAME_ID_PARAM = "gameId";
    private static final String RADIO_TOKEN_URL = "/api/games/{gameId}/radio/token";

    private User chaser;
    private User runner;
    private Game game;
    private GameParticipant chaserParticipant;
    private String chaserToken;
    private String runnerToken;

    @Autowired
    private RadioOverloadRepository radioOverloadRepository;

    @BeforeEach
    void setUp() {
        chaser = givenUser("chaser");
        runner = givenUser("runner");

        chaserToken = givenAccessToken(chaser);
        runnerToken = givenAccessToken(runner);

        game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        chaserParticipant = givenChaser(game, chaser);
        givenRunner(game, runner);
    }

    @AfterEach
    void tearDown() {
        radioOverloadRepository.deactivate();
    }

    @Nested
    @DisplayName("무전 접속 토큰 발급 API")
    class IssueToken {

        @Test
        void 무전_토큰_발급_성공() {
            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(RADIO_TOKEN_URL)
                    .then()
                    .extract();

            // then
            RadioTokenResponse result = response.as(RadioTokenResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.url()).isEqualTo("wss://dummy.livekit.cloud");
                softly.assertThat(result.room()).isEqualTo("dev-game-" + game.getId() + "-chaser");
                softly.assertThat(result.identity()).isEqualTo(chaserParticipant.getId().toString());
                softly.assertThat(result.token()).isNotBlank();
                softly.assertThat(result.expiresAt()).endsWith("+09:00");
            });
        }

        @Test
        void 도둑이면_무전을_쓸_수_없으며_403_Forbidden을_응답한다() {
            // when
            ExtractableResponse<Response> response = authenticated(runnerToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(RADIO_TOKEN_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void 무전_서버_과부하_중_신규_입장이면_503_Service_Unavailable을_응답한다() {
            // given
            radioOverloadRepository.activate(Duration.ofMinutes(10));

            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(RADIO_TOKEN_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }
}
