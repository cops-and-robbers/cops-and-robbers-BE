package com.team.cops_and_robbers.play.system.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.play.system.presentation.dto.request.ArrestRequest;
import com.team.cops_and_robbers.play.system.presentation.dto.response.ArrestResponse;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class SystemControllerTest extends ControllerTest {

    private static final String GAME_ID_PARAM = "gameId";
    private static final String ARREST_URL = "/api/games/{gameId}/system/arrest";
    private static final String ESCAPE_URL = "/api/games/{gameId}/system/escape";

    private User police;
    private User robber;
    private Game game;
    private GameParticipant policeParticipant;
    private GameParticipant robberParticipant;
    private String policeToken;
    private String robberToken;

    @BeforeEach
    void setUp() {
        police = givenUser("police");
        robber = givenUser("robber");

        policeToken = givenAccessToken(police);
        robberToken = givenAccessToken(robber);

        game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));
        policeParticipant = givenPolice(game, police);
        robberParticipant = givenRobber(game, robber);
    }

    @Nested
    @DisplayName("도둑 체포 API")
    class ArrestRobber {

        @Test
        void 도둑_체포_성공() {
            // given
            User secondRobber = givenUser("robber2");
            givenRobber(game, secondRobber);
            ArrestRequest request = new ArrestRequest(robberParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            ArrestResponse result = response.as(ArrestResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.robberNickname()).isEqualTo(robber.getNickname());
                softly.assertThat(result.remainingThieves()).isEqualTo(1);

                GameParticipant jailedRobber = gameParticipantRepository.findById(robberParticipant.getId()).orElseThrow();
                softly.assertThat(jailedRobber.isJailed()).isTrue();
            });
        }

        @Test
        void 모든_도둑이_체포되면_게임이_종료되어_게임과_유저는_WAITING_상태가_되어야한다() {
            // given
            ArrestRequest request = new ArrestRequest(robberParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            ArrestResponse result = response.as(ArrestResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.robberNickname()).isEqualTo(robber.getNickname());
                softly.assertThat(result.remainingThieves()).isEqualTo(0);

                GameParticipant jailedRobber = gameParticipantRepository.findById(robberParticipant.getId()).orElseThrow();
                softly.assertThat(jailedRobber.isWaiting()).isTrue();

                gameRepository.findById(game.getId()).orElseThrow();
                softly.assertThat(game.getStatus() == GameStatus.WAITING);

                softly.assertThat(gameResultRepository.existsById(1L)).isTrue();    // 게임 결과 또한 저장되어야 한다.
            });
        }

        @Test
        void 게임이_진행_중이_아니면_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            givenPolice(waitingGame, police);
            GameParticipant waitingRobber = givenRobber(waitingGame, robber);

            ArrestRequest request = new ArrestRequest(waitingRobber.getId());

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 경찰이_아니면_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given (도둑이 요청)
            ArrestRequest request = new ArrestRequest(robberParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(robberToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 경찰이_대기_상태이면_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given
            policeParticipant.updateStatus(ParticipantStatus.POLICE_WAITING);
            gameParticipantRepository.save(policeParticipant);

            ArrestRequest request = new ArrestRequest(robberParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 대상이_도둑이_아니면_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given (경찰을 체포 시도)
            ArrestRequest request = new ArrestRequest(policeParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 이미_체포된_도둑은_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given
            robberParticipant.updateStatus(ParticipantStatus.JAILED);
            gameParticipantRepository.save(robberParticipant);

            ArrestRequest request = new ArrestRequest(robberParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 다른_게임_참가자를_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game otherGame = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
            User otherUser = givenUser("other");
            GameParticipant otherRobberParticipant = givenRobber(otherGame, otherUser);

            ArrestRequest request = new ArrestRequest(otherRobberParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // given
            ArrestRequest request = new ArrestRequest(robberParticipant.getId());

            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void 존재하지_않는_참가자를_체포하려하면_404_NotFound를_응답한다() {
            // given
            ArrestRequest request = new ArrestRequest(9999L); // 존재 X

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("도둑 탈옥 API")
    class EscapeFromPrison {

        @Test
        void 도둑_탈옥_성공() {
            // given
            robberParticipant.updateStatus(ParticipantStatus.JAILED);
            gameParticipantRepository.save(robberParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(robberToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ESCAPE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

            GameParticipant escapedRobber = gameParticipantRepository.findById(robberParticipant.getId()).orElseThrow();
            assertThat(escapedRobber.isJailed()).isFalse();
            assertThat(escapedRobber.isRobber()).isTrue();
        }

        @Test
        void 게임이_진행_중이_아니면_탈옥할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            GameParticipant waitingRobber = givenRobber(waitingGame, robber);
            waitingRobber.updateStatus(ParticipantStatus.JAILED);
            gameParticipantRepository.save(waitingRobber);

            // when
            ExtractableResponse<Response> response = authenticated(robberToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .post(ESCAPE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 도둑이_아니면_탈옥할_수_없으며_400_BadRequest를_응답한다() {
            // given
            // policeParticipant는 경찰 팀

            // when (경찰이 요청)
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ESCAPE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 수감되지_않은_도둑은_탈옥할_수_없으며_400_BadRequest를_응답한다() {
            // given
            assertThat(robberParticipant.getStatus()).isEqualTo(ParticipantStatus.ALIVE); // ALIVE 상태 확인

            // when
            ExtractableResponse<Response> response = authenticated(robberToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ESCAPE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ESCAPE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void 존재하지_않는_게임에_요청하면_404_NotFound를_응답한다() {
            // when
            ExtractableResponse<Response> response = authenticated(robberToken)
                    .pathParam(GAME_ID_PARAM, 9999L) // 존재 X
                    .when()
                    .post(ESCAPE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }
}
