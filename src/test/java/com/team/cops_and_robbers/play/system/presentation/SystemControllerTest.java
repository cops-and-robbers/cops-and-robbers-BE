package com.team.cops_and_robbers.play.system.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.play.system.presentation.dto.ArrestRequest;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        policeParticipant = givenPolice(game, police);
        robberParticipant = givenRobber(game, robber);
    }

    @Nested
    @DisplayName("도둑 체포 API")
    class ArrestRobber {

        @Test
        void 도둑_체포_성공() {
            // given
            ArrestRequest request = new ArrestRequest(robberParticipant.getId());

            // when
            ExtractableResponse<Response> response = post(
                    ARREST_URL,
                    policeToken,
                    request,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            GameParticipant jailedRobber = gameParticipantRepository.findById(robberParticipant.getId()).orElseThrow();
            assertThat(jailedRobber.isJailed()).isTrue();
        }

        @Test
        void 경찰이_아니면_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given (도둑이 요청)
            ArrestRequest request = new ArrestRequest(robberParticipant.getId());

            // when
            ExtractableResponse<Response> response = post(
                    ARREST_URL,
                    robberToken,
                    request,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 대상이_도둑이_아니면_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given (경찰을 체포 시도)
            ArrestRequest request = new ArrestRequest(policeParticipant.getId());

            // when
            ExtractableResponse<Response> response = post(
                    ARREST_URL,
                    policeToken,
                    request,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

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
            ExtractableResponse<Response> response = post(
                    ARREST_URL,
                    policeToken,
                    request,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

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
            ExtractableResponse<Response> response = post(
                    ARREST_URL,
                    policeToken,
                    request,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // given
            ArrestRequest request = new ArrestRequest(robberParticipant.getId());

            // when
            ExtractableResponse<Response> response = postWithoutAuthorization(
                    ARREST_URL,
                    request,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void 존재하지_않는_참가자를_체포하려하면_404_NotFound를_응답한다() {
            // given
            ArrestRequest request = new ArrestRequest(9999L); // 존재 X

            // when
            ExtractableResponse<Response> response = post(
                    ARREST_URL,
                    policeToken,
                    request,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

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
            ExtractableResponse<Response> response = post(
                    ESCAPE_URL,
                    robberToken,
                    null,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            GameParticipant escapedRobber = gameParticipantRepository.findById(robberParticipant.getId()).orElseThrow();
            assertThat(escapedRobber.isJailed()).isFalse();
            assertThat(escapedRobber.isRobber()).isTrue();
        }

        @Test
        void 도둑이_아니면_탈옥할_수_없으며_400_BadRequest를_응답한다() {
            // given
            // policeParticipant는 경찰 팀

            // when (경찰이 요청)
            ExtractableResponse<Response> response = post(
                    ESCAPE_URL,
                    policeToken,
                    null,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 수감되지_않은_도둑은_탈옥할_수_없으며_400_BadRequest를_응답한다() {
            // given
            assertThat(robberParticipant.getStatus()).isEqualTo(ParticipantStatus.ALIVE); // ALIVE 상태 확인

            // when
            ExtractableResponse<Response> response = post(
                    ESCAPE_URL,
                    robberToken,
                    null,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // when
            ExtractableResponse<Response> response = postWithoutAuthorization(
                    ESCAPE_URL,
                    null,
                    Map.of(GAME_ID_PARAM, game.getId())
            );

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        void 존재하지_않는_게임에_요청하면_404_NotFound를_응답한다() {
            // when
            ExtractableResponse<Response> response = post(
                    ESCAPE_URL,
                    robberToken,
                    null,
                    Map.of(GAME_ID_PARAM, 9999L) // 존재 X
            );

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }
}
