package com.team.cops_and_robbers.play.location.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.play.common.application.InGameParticipantCacheService;
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

class RobberLocationControllerTest extends ControllerTest {

    private static final String ROBBER_LOCATION_URL = "/api/games/{gameId}/robbers/location";

    @Autowired
    private RobberLocationService robberLocationService;

    @Autowired
    private InGameParticipantCacheService inGameParticipantCacheService;

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

        inGameParticipantCacheService.clearCache(game.getId());
        inGameParticipantCacheService.loadCache(game.getId());
        robberLocationService.clearRobberLocations(game.getId());
        robberLocationService.clearRobberLocationSnapshot(game.getId());
    }

    @Nested
    @DisplayName("도둑 위치 조회 API")
    class GetRobberLocations {

        @Test
        void 도둑위치는_스냅샷_기준으로_위치가_반환된다() {
            // given: 위치 업데이트 후 공개(스냅샷 저장) 시뮬레이션
            robberLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), robberParticipant.getId(), 37.5665, 126.9780)
            );
            robberLocationService.revealRobberLocations(game.getId());

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
        void 첫_위치_공개_전에는_빈_목록이_반환된다() {
            // given: 도둑이 위치를 전송했지만 아직 공개 주기가 도래하지 않은 상태
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
                softly.assertThat(results).isEmpty();
            });
        }

        @Test
        void 공개_후_도둑이_이동해도_스냅샷_기준_위치가_반환된다() {
            // given: 첫 번째 위치로 공개(스냅샷), 이후 다른 위치로 이동
            robberLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), robberParticipant.getId(), 37.5665, 126.9780)
            );
            robberLocationService.revealRobberLocations(game.getId());
            robberLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), robberParticipant.getId(), 37.9999, 127.9999)
            );

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .when()
                    .get(ROBBER_LOCATION_URL, game.getId())
                    .then()
                    .extract();

            // then: 스냅샷(공개 시점) 위치 반환, 이동 후 실시간 위치 미반영
            RobberLocationResponse[] results = response.as(RobberLocationResponse[].class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(results).hasSize(1);
                softly.assertThat(results[0].latitude()).isEqualTo(37.5665);
                softly.assertThat(results[0].longitude()).isEqualTo(126.9780);
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
        void 게임이_진행_중이_아니면_400_BadRequest를_응답한다() {
            // given
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            givenPolice(waitingGame, police);

            // when
            ExtractableResponse<Response> response = authenticated(policeToken)
                    .when()
                    .get(ROBBER_LOCATION_URL, waitingGame.getId())
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(400);
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
