package com.team.cops_and_robbers.play.location.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.location.presentation.dto.RobberLocationResponse;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class LocationControllerTest extends ControllerTest {

    private static final String ROBBER_LOCATION_URL = "/api/games/{gameId}/robbers/location";

    @Autowired
    private RobberLocationService robberLocationService;

    private User police;
    private User robber;
    private Game game;
    private GameParticipant policeParticipant;
    private GameParticipant robberParticipant;
    private String policeToken;

    @BeforeEach
    void setUp() {
        police = givenUser("police");
        robber = givenUser("robber");

        policeToken = givenAccessToken(police);

        game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        policeParticipant = givenPolice(game, police);
        robberParticipant = givenRobber(game, robber);

        robberLocationService.clearRobberLocations(game.getId());
    }

    @Nested
    @DisplayName("도둑 위치 조회 API")
    class GetRobberLocations {

        @Test
        void 위치를_전송한_도둑의_위치가_반환된다() {
            // given
            robberLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), robberParticipant.getId(), 37.5665, 126.9780)
            );

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .when()
                    .get(ROBBER_LOCATION_URL, game.getId())
                    .then()
                    .extract();

            // then
            RobberLocationResponse[] results = response.as(RobberLocationResponse[].class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(results).hasSize(1);
                softly.assertThat(results[0].participantId()).isEqualTo(robberParticipant.getId());
                softly.assertThat(results[0].nickname()).isEqualTo(robber.getNickname());
                softly.assertThat(results[0].latitude()).isEqualTo(37.5665);
                softly.assertThat(results[0].longitude()).isEqualTo(126.9780);
            });
        }

        @Test
        void 위치를_한_번도_전송하지_않은_경우_빈_목록이_반환된다() {
            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .when()
                    .get(ROBBER_LOCATION_URL, game.getId())
                    .then()
                    .extract();

            // then
            RobberLocationResponse[] results = response.as(RobberLocationResponse[].class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(results).isEmpty();
            });
        }

        @Test
        void 위치를_여러_번_전송한_경우_마지막_위치만_반환된다() {
            // given
            robberLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), robberParticipant.getId(), 37.5665, 126.9780)
            );
            robberLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), robberParticipant.getId(), 37.9999, 127.9999)
            );

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .when()
                    .get(ROBBER_LOCATION_URL, game.getId())
                    .then()
                    .extract();

            // then
            RobberLocationResponse[] results = response.as(RobberLocationResponse[].class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(results).hasSize(1);
                softly.assertThat(results[0].latitude()).isEqualTo(37.9999);
                softly.assertThat(results[0].longitude()).isEqualTo(127.9999);
            });
        }

        @Test
        void 게임이_존재하지_않으면_404_NotFound를_응답한다() {
            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .when()
                    .get(ROBBER_LOCATION_URL, game.getId() + 999)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void 해당_게임의_참가자가_아니면_404_NotFound를_응답한다() {
            // given
            User outsider = givenUser("outsider");
            String outsiderToken = givenAccessToken(outsider);

            // when
            ExtractableResponse<Response> response = authenticated(outsiderToken)
                    .when()
                    .get(ROBBER_LOCATION_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .when()
                    .get(ROBBER_LOCATION_URL, game.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(401);
        }
    }
}
