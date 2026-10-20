package com.team.cops_and_robbers.history.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.team.cops_and_robbers.common.fixture.GameFixture.FINISHED_GAME;
import static com.team.cops_and_robbers.common.fixture.GameResultFixture.POLICE_WIN_RESULT;
import static com.team.cops_and_robbers.common.fixture.GameResultParticipantFixture.POLICE_PARTICIPANT;
import static com.team.cops_and_robbers.common.fixture.GameResultParticipantFixture.ROBBER_PARTICIPANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GameResultControllerTest extends ControllerTest {

    private static final String GAME_RESULT_API_URL = "/api/game-results/{gameResultId}";

    private User user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = givenUser();
        accessToken = givenAccessToken(user);
    }

    @Nested
    @DisplayName("게임 결과 조회 API")
    class GetGameResult {

        @Test
        void 게임_결과_조회에_성공한다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            givenPolice(game, user);
            GameResult gameResult = gameResultRepository.save(POLICE_WIN_RESULT(game.getId()));

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_RESULT_API_URL, gameResult.getId())
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("winnerTeam")).isEqualTo("POLICE");
                softly.assertThat(response.jsonPath().getInt("durationSeconds")).isEqualTo(300);
                softly.assertThat(response.jsonPath().getInt("totalArrestCount")).isEqualTo(5);
                softly.assertThat(response.jsonPath().getInt("remainingRobberCount")).isEqualTo(0);
            });
        }

        @Test
        void 팀별_MVP_닉네임이_함께_내려온다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            givenPolice(game, user);
            GameResult gameResult = gameResultRepository.save(POLICE_WIN_RESULT(game.getId()));

            GameResultParticipant police = POLICE_PARTICIPANT(gameResult, user.getId(), "날쌘경찰");
            police.markMvp();
            GameResultParticipant robber = ROBBER_PARTICIPANT(gameResult, 999L, "빠른도둑");
            robber.markMvp();
            gameResultParticipantRepository.saveAll(List.of(police, robber));

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_RESULT_API_URL, gameResult.getId())
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("policeMvp.nickname")).isEqualTo("날쌘경찰");
                softly.assertThat(response.jsonPath().getString("robberMvp.nickname")).isEqualTo("빠른도둑");
            });
        }

        @Test
        void MVP가_없으면_null로_내려온다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            givenPolice(game, user);
            GameResult gameResult = gameResultRepository.save(POLICE_WIN_RESULT(game.getId()));
            gameResultParticipantRepository.save(POLICE_PARTICIPANT(gameResult, user.getId(), "그냥경찰"));

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_RESULT_API_URL, gameResult.getId())
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("policeMvp")).isNull();
                softly.assertThat(response.jsonPath().getString("robberMvp")).isNull();
            });
        }

        @Test
        void 해당_게임의_참가자가_아니면_404_NotFound를_응답한다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            GameResult gameResult = gameResultRepository.save(POLICE_WIN_RESULT(game.getId()));

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_RESULT_API_URL, gameResult.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void 게임_결과가_존재하지_않으면_404_NotFound를_응답한다() {
            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(GAME_RESULT_API_URL, 999L)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            GameResult gameResult = gameResultRepository.save(POLICE_WIN_RESULT(game.getId()));

            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .when()
                    .get(GAME_RESULT_API_URL, gameResult.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("내 게임 기록 조회 API")
    class GetMyGameRecord {

        private static final String MY_RECORD_API_URL = "/api/game-results/{gameResultId}/me";

        @Test
        void MVP로_선정되면_isMvp가_true로_내려온다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            givenPolice(game, user);
            GameResult gameResult = gameResultRepository.save(POLICE_WIN_RESULT(game.getId()));
            GameResultParticipant police = POLICE_PARTICIPANT(gameResult, user.getId(), "날쌘경찰");
            police.markMvp();
            gameResultParticipantRepository.save(police);

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(MY_RECORD_API_URL, gameResult.getId())
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("nickname")).isEqualTo("날쌘경찰");
                softly.assertThat(response.jsonPath().getBoolean("isMvp")).isTrue();
            });
        }

        @Test
        void MVP가_아니면_isMvp가_false로_내려온다() {
            // given
            Game game = gameRepository.save(FINISHED_GAME());
            givenPolice(game, user);
            GameResult gameResult = gameResultRepository.save(POLICE_WIN_RESULT(game.getId()));
            gameResultParticipantRepository.save(POLICE_PARTICIPANT(gameResult, user.getId(), "그냥경찰"));

            // when
            ExtractableResponse<Response> response = authenticated(accessToken)
                    .when()
                    .get(MY_RECORD_API_URL, gameResult.getId())
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getBoolean("isMvp")).isFalse();
            });
        }
    }
}
