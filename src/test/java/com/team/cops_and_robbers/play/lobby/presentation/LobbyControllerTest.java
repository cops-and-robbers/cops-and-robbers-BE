package com.team.cops_and_robbers.play.lobby.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.lobby.presentation.dto.ReadyUpdateRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.TeamChangeRequest;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LobbyControllerTest extends ControllerTest {

    private static final String GAME_ID_PARAM = "gameId";
    private static final String TEAM_CHANGE_URL = "/api/games/{gameId}/lobby/team";
    private static final String READY_UPDATE_URL = "/api/games/{gameId}/lobby/ready";
    private static final String GAME_START_URL = "/api/games/{gameId}/lobby/start";

    private User host;
    private User guest;
    private Game game;
    private GameParticipant hostParticipant;
    private GameParticipant guestParticipant;
    private String hostToken;
    private String guestToken;

    @BeforeEach
    void setUp() {
        host = givenUser("host");
        guest = givenUser("guest");

        hostToken = givenAccessToken(host);
        guestToken = givenAccessToken(guest);

        game = gameRepository.save(GameFixture.WAITING_GAME());
        hostParticipant = givenHost(game, host);
        guestParticipant = givenGuest(game, guest);
    }

    @Nested
    @DisplayName("팀 변경 API")
    class ChangeTeam {

        @Test
        void 팀_변경_성공() {
            // given
            TeamChangeRequest request = new TeamChangeRequest(Team.POLICE);

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .patch(TEAM_CHANGE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

            GameParticipant updatedParticipant = gameParticipantRepository.findById(guestParticipant.getId()).orElseThrow();
            assertThat(updatedParticipant.getTeam()).isEqualTo(Team.POLICE);
            assertThat(updatedParticipant.isReady()).isFalse();
        }

        @Test
        void 이미_시작된_게임에서는_팀을_변경할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME(UUID.randomUUID().toString().substring(0, 6)));
            waitingGame.startGame(java.time.LocalDateTime.now());
            gameRepository.save(waitingGame);

            gameParticipantRepository.save(GameParticipantFixture.GUEST_PARTICIPANT(waitingGame, guest));

            TeamChangeRequest request = new TeamChangeRequest(Team.POLICE);

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .patch(TEAM_CHANGE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // given
            TeamChangeRequest request = new TeamChangeRequest(Team.POLICE);

            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .patch(TEAM_CHANGE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void 존재하지_않는_게임이면_404_NotFound를_응답한다() {
            // given
            TeamChangeRequest request = new TeamChangeRequest(Team.POLICE);

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, 9999L)
                    .when()
                    .patch(TEAM_CHANGE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("준비 상태 변경 API")
    class UpdateReady {

        @Test
        void 준비_상태_변경_성공() {
            // given
            ReadyUpdateRequest request = new ReadyUpdateRequest(true);

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .patch(READY_UPDATE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

            GameParticipant updatedParticipant = gameParticipantRepository.findById(guestParticipant.getId()).orElseThrow();
            assertThat(updatedParticipant.isReady()).isTrue();
        }

        @Test
        void 방장은_준비_상태를_해제할_수_없으며_400_BadRequest를_응답한다() {
            // given
            ReadyUpdateRequest request = new ReadyUpdateRequest(false);

            // when
            ExtractableResponse<Response> response = authenticated(hostToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .patch(READY_UPDATE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 이미_시작된_게임에서는_준비_상태를_변경할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME(UUID.randomUUID().toString().substring(0, 6)));
            waitingGame.startGame(java.time.LocalDateTime.now());
            gameRepository.save(waitingGame);

            gameParticipantRepository.save(GameParticipantFixture.GUEST_PARTICIPANT(waitingGame, guest));

            ReadyUpdateRequest request = new ReadyUpdateRequest(true);

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .patch(READY_UPDATE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // given
            ReadyUpdateRequest request = new ReadyUpdateRequest(true);

            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .body(request)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .patch(READY_UPDATE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Nested
    @DisplayName("게임 시작 API")
    class StartGame {

        @Test
        void 게임_시작_성공() {
            // given
            guestParticipant.updateReady(true); // 방장 제외 모든 인원 준비 완료 시
            gameParticipantRepository.save(guestParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(hostToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(GAME_START_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

            Game startedGame = gameRepository.findById(game.getId()).orElseThrow();
            assertThat(startedGame.isInProgress()).isTrue();
        }

        @Test
        void 방장이_아니면_403_Forbidden을_응답한다() {
            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(GAME_START_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        void 경찰이_없으면_400_BadRequest를_응답한다() {
            // given
            guestParticipant.changeTeam(Team.ROBBER);
            guestParticipant.updateReady(true);
            gameParticipantRepository.save(guestParticipant);

            hostParticipant.changeTeam(Team.ROBBER);
            gameParticipantRepository.save(hostParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(hostToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(GAME_START_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 준비하지_않은_참가자가_있으면_400_BadRequest를_응답한다() {
            // given
            guestParticipant.changeTeam(Team.POLICE);
            gameParticipantRepository.save(guestParticipant);

            hostParticipant.changeTeam(Team.ROBBER);
            gameParticipantRepository.save(hostParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(hostToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(GAME_START_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 이미_시작된_게임에서는_게임을_시작할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME(UUID.randomUUID().toString().substring(0, 6)));
            waitingGame.startGame(java.time.LocalDateTime.now());
            gameRepository.save(waitingGame);

            givenHost(waitingGame, host);

            // when
            ExtractableResponse<Response> response = authenticated(hostToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .post(GAME_START_URL)
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
                    .post(GAME_START_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
