package com.team.cops_and_robbers.game.participant.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.presentation.dto.request.GameJoinRequest;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameJoinResponse;
import com.team.cops_and_robbers.game.participant.presentation.dto.response.GameLeaveResponse;
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
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GameParticipantControllerTest extends ControllerTest {

    private static final String GAME_ID_PARAM = "gameId";
    private static final String JOIN_URL = "/api/games/join";
    private static final String LEAVE_URL = "/api/games/{gameId}/leave";

    private User host;
    private Game waitingGame;
    private String hostToken;

    @BeforeEach
    void setUp() {
        host = givenUser("host");
        hostToken = givenAccessToken(host);
        waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
        givenHost(waitingGame, host);
    }

    @Nested
    @DisplayName("게임 방 참여 API")
    class JoinGame {

        @Test
        void 게임_방_참여_성공() {
            // given
            User guest = givenUser("guest");
            String guestToken = givenAccessToken(guest);
            GameJoinRequest request = new GameJoinRequest(waitingGame.getInviteCode());

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .body(request)
                    .when()
                    .post(JOIN_URL)
                    .then()
                    .extract();

            // then
            GameJoinResponse result = response.as(GameJoinResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.gameId()).isEqualTo(waitingGame.getId());
                softly.assertThat(result.participantId()).isNotNull();
            });
        }

        @Test
        void 잘못된_초대_코드로_참여하면_400_BadRequest를_응답한다() {
            // given
            User guest = givenUser("guest");
            String guestToken = givenAccessToken(guest);
            GameJoinRequest request = new GameJoinRequest("000000");

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .body(request)
                    .when()
                    .post(JOIN_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 이미_시작된_게임에_참여하면_400_BadRequest를_응답한다() {
            // given
            Game startedGame = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
            gameRepository.save(startedGame);

            User guest = givenUser("guest");
            String guestToken = givenAccessToken(guest);
            GameJoinRequest request = new GameJoinRequest(startedGame.getInviteCode());

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .body(request)
                    .when()
                    .post(JOIN_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 최대_참여_인원_초과_시_400_BadRequest를_응답한다() {
            // given
            Game fullGame = gameRepository.save(
                    Game.builder()
                            .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                            .status(GameStatus.WAITING)
                            .roundDurationMinutes(30)
                            .locationRevealIntervalMinutes(5)
                            .policeWaitMinutes(3)
                            .maxParticipants(2) // 최대 인원 2
                            .build()
            );
            givenHost(fullGame, host);

            User guest1 = givenUser("guest1");
            givenGuest(fullGame, guest1); // 최대 인원 2 도달

            User guest2 = givenUser("guest2");
            String guest2Token = givenAccessToken(guest2);
            GameJoinRequest request = new GameJoinRequest(fullGame.getInviteCode());

            // when
            ExtractableResponse<Response> response = authenticated(guest2Token)
                    .body(request)
                    .when()
                    .post(JOIN_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // given
            GameJoinRequest request = new GameJoinRequest(waitingGame.getInviteCode());

            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .body(request)
                    .when()
                    .post(JOIN_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void 이미_다른_활성_게임에_참여_중이면_409_Conflict를_응답한다() {
            // given
            User guest = givenUser("guest");
            String guestToken = givenAccessToken(guest);
            givenGuest(waitingGame, guest);

            Game anotherGame = gameRepository.save(GameFixture.WAITING_GAME(UUID.randomUUID().toString().substring(0, 6)));
            GameJoinRequest request = new GameJoinRequest(anotherGame.getInviteCode());

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .body(request)
                    .when()
                    .post(JOIN_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        }
    }

    @Nested
    @DisplayName("게임 방 퇴장 API")
    class LeaveGame {

        @Test
        void 게임_방_퇴장_성공() {
            // given
            User guest = givenUser("guest");
            String guestToken = givenAccessToken(guest);
            givenGuest(waitingGame, guest);

            // when
            ExtractableResponse<Response> response = authenticated(guestToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .delete(LEAVE_URL)
                    .then()
                    .extract();

            // then
            GameLeaveResponse result = response.as(GameLeaveResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.leftUserId()).isEqualTo(guest.getId());
                softly.assertThat(result.remainingCount()).isEqualTo(1);
            });
        }

        @Test
        void 방장이_퇴장하면_다음_참여자에게_방장이_이전된다() {
            // given
            User guest = givenUser("guest");
            givenGuest(waitingGame, guest);

            // when
            ExtractableResponse<Response> response = authenticated(hostToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .delete(LEAVE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            GameParticipant newHost = gameParticipantRepository.findByGameIdAndUserId(waitingGame.getId(), guest.getId())
                    .orElseThrow();
            assertThat(newHost.isHost()).isTrue();
        }

        @Test
        void 마지막_참여자가_퇴장하면_게임_방이_삭제된다() {
            // when
            ExtractableResponse<Response> response = authenticated(hostToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .delete(LEAVE_URL)
                    .then()
                    .extract();

            // then
            GameLeaveResponse result = response.as(GameLeaveResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.remainingCount()).isEqualTo(0);
            });

            assertThat(gameRepository.findById(waitingGame.getId())).isEmpty();
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .delete(LEAVE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void 존재하지_않는_게임에서_퇴장하면_404_NotFound를_응답한다() {
            // when
            ExtractableResponse<Response> response = authenticated(hostToken)
                    .pathParam(GAME_ID_PARAM, 9999L)
                    .when()
                    .delete(LEAVE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void 참여하지_않은_게임에서_퇴장하면_404_NotFound를_응답한다() {
            // given
            User nonParticipant = givenUser("nonParticipant");
            String nonParticipantToken = givenAccessToken(nonParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(nonParticipantToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .delete(LEAVE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }
}
