package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.common.fixture.GameResultFixture;
import com.team.cops_and_robbers.community.post.infrastructure.GeocodingResult;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AdminGameResolverTest extends ControllerTest {

    private static final String GRAPHQL_URL = "/graphql";

    private String adminToken;

    @BeforeEach
    void setUpAdmin() {
        adminToken = givenAccessToken(givenAdminUser("admin"));
    }

    @Nested
    @DisplayName("인증 검사")
    class Authentication {

        @Test
        void 토큰_없이_요청하면_401을_반환한다() {
            // when
            ExtractableResponse<Response> response = executeQuery(
                    "{ adminGames { totalElements content { id status } } }");

            // then
            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        void 일반_유저_토큰으로_요청하면_403을_반환한다() {
            // given
            User user = givenUser("normalUser");
            String userToken = givenAccessToken(user);

            // when
            ExtractableResponse<Response> response = executeQuery(
                    userToken, "{ adminGames { totalElements content { id status } } }");

            // then
            assertThat(response.statusCode()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("게임 목록 조회")
    class AdminGames {

        @Test
        void 게임_목록을_페이지로_조회한다() {
            // given
            gameRepository.save(GameFixture.FINISHED_GAME());
            gameRepository.save(GameFixture.WAITING_GAME());

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGames(page: 0, size: 10) { totalElements totalPages page size content { id inviteCode status participantCount } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminGames.totalElements")).isGreaterThanOrEqualTo(2);
                softly.assertThat(response.jsonPath().getList("data.adminGames.content")).isNotEmpty();
            });
        }

        @Test
        void 상태로_게임을_필터링한다() {
            // given
            gameRepository.save(GameFixture.FINISHED_GAME());
            gameRepository.save(GameFixture.WAITING_GAME());

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGames(status: FINISHED) { totalElements content { id status } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminGames.totalElements")).isGreaterThanOrEqualTo(1);
                softly.assertThat(response.jsonPath().getString("data.adminGames.content[0].status")).isEqualTo("FINISHED");
            });
        }

        @Test
        void 게임_모드로_게임을_필터링한다() {
            // given
            gameRepository.save(GameFixture.WAITING_GAME());
            gameRepository.save(GameFixture.WAITING_TAG_GAME());

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGames(gameMode: TAG) { totalElements content { id gameMode } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getList("data.adminGames.content.gameMode", String.class))
                        .isNotEmpty()
                        .containsOnly("TAG");
            });
        }

        @Test
        void 결과가_없으면_빈_목록을_반환한다() {
            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGames(status: IN_PROGRESS) { totalElements content { id } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminGames.totalElements")).isZero();
                softly.assertThat(response.jsonPath().getList("data.adminGames.content")).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("게임 기록 목록 조회")
    class AdminGameHistories {

        @Test
        void 게임_모드로_게임_기록을_필터링한다() {
            // given
            Game game = gameRepository.save(GameFixture.FINISHED_GAME());
            Game tagGame = gameRepository.save(GameFixture.FINISHED_GAME());
            GameResult tagResult = GameResultFixture.POLICE_WIN_RESULT_WITHOUT_JAIL(tagGame.getId());
            ReflectionTestUtils.setField(tagResult, "gameMode", GameMode.TAG);
            gameResultRepository.save(GameResultFixture.POLICE_WIN_RESULT(game.getId()));
            gameResultRepository.save(tagResult);

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGameHistories(gameMode: TAG) { totalElements content { gameId gameMode } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getList("data.adminGameHistories.content.gameMode", String.class))
                        .isNotEmpty()
                        .containsOnly("TAG");
            });
        }
    }

    @Nested
    @DisplayName("게임 단건 조회")
    class AdminGame {

        @Test
        void 게임_아이디로_단건_조회한다() {
            // given
            Game game = gameRepository.save(GameFixture.FINISHED_GAME());

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGame(id: " + game.getId() + ") { id inviteCode status roundDurationMinutes maxParticipants } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.adminGame.id")).isEqualTo(String.valueOf(game.getId()));
                softly.assertThat(response.jsonPath().getString("data.adminGame.status")).isEqualTo("FINISHED");
            });
        }

        @Test
        void 참여자_목록을_포함하여_조회한다() {
            // given
            Game game = gameRepository.save(GameFixture.FINISHED_GAME());
            User user = givenUser("participant");
            gameParticipantRepository.save(GameParticipantFixture.HOST_PARTICIPANT(game, user));

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGame(id: " + game.getId() + ") { id participants { userId nickname team status isHost } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getList("data.adminGame.participants")).hasSize(1);
                softly.assertThat(response.jsonPath().getString("data.adminGame.participants[0].nickname")).isEqualTo("participant");
                softly.assertThat((Boolean) response.jsonPath().get("data.adminGame.participants[0].isHost")).isTrue();
            });
        }

        @Test
        void 게임_결과를_포함하여_조회한다() {
            // given
            Game game = gameRepository.save(GameFixture.FINISHED_GAME());
            gameResultRepository.save(GameResultFixture.POLICE_WIN_RESULT(game.getId()));

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGame(id: " + game.getId() + ") { id result { winnerTeam endReason totalPoliceCount totalRobberCount durationSeconds } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.adminGame.result.winnerTeam")).isEqualTo("POLICE");
                softly.assertThat(response.jsonPath().getString("data.adminGame.result.endReason")).isEqualTo("ALL_ARRESTED");
            });
        }

        @Test
        void 게임_결과가_없으면_result가_null이다() {
            // given
            Game game = gameRepository.save(GameFixture.WAITING_GAME());

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGame(id: " + game.getId() + ") { id result { winnerTeam } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Object) response.jsonPath().get("data.adminGame.result")).isNull();
            });
        }

        @Test
        void 게임_구역을_포함하여_조회한다() {
            // given
            Game game = gameRepository.save(GameFixture.FINISHED_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGame(id: " + game.getId() + ") { id area { areaType playgroundCenterLat playgroundCenterLng playgroundRadiusInMeters jailCenterLat jailCenterLng jailRadiusInMeters } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Object) response.jsonPath().get("data.adminGame.area")).isNotNull();
                softly.assertThat((Integer) response.jsonPath().get("data.adminGame.area.playgroundRadiusInMeters")).isEqualTo(500);
            });
        }

        @Test
        void 게임_구역이_없으면_area가_null이다() {
            // given
            Game game = gameRepository.save(GameFixture.WAITING_GAME());

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGame(id: " + game.getId() + ") { id area { playgroundRadiusInMeters } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Object) response.jsonPath().get("data.adminGame.area")).isNull();
            });
        }
    }

    @Nested
    @DisplayName("구역 동네 이름 조회")
    class PlaceName {

        private static final String PLACE_NAME = "서울특별시 광진구 화양동";

        @Test
        void 게임_목록에_구역의_동네_이름이_내려온다() {
            // given
            Game game = gameRepository.save(GameFixture.WAITING_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));
            Game gameWithoutArea = gameRepository.save(GameFixture.WAITING_GAME());

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGames { content { id placeName } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Object) response.jsonPath().get("errors")).isNull();
                softly.assertThat(response.jsonPath().getString(
                        "data.adminGames.content.find { it.id == '" + game.getId() + "' }.placeName")).isEqualTo(PLACE_NAME);
                softly.assertThat(response.jsonPath().getString(
                        "data.adminGames.content.find { it.id == '" + gameWithoutArea.getId() + "' }.placeName")).isNull();
            });
        }

        @Test
        void 한_번_찾은_동네_이름은_저장해_두고_다시_조회하지_않는다() {
            // given
            Game game = gameRepository.save(GameFixture.WAITING_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));
            String query = "{ adminGame(id: " + game.getId() + ") { area { placeName } } }";

            // when
            ExtractableResponse<Response> first = executeQuery(adminToken, query);
            ExtractableResponse<Response> second = executeQuery(adminToken, query);

            // then
            assertSoftly(softly -> {
                softly.assertThat(first.jsonPath().getString("data.adminGame.area.placeName")).isEqualTo(PLACE_NAME);
                softly.assertThat(second.jsonPath().getString("data.adminGame.area.placeName")).isEqualTo(PLACE_NAME);
                softly.assertThat(areaPlaceNameRepository.findAll()).hasSize(1);
            });
            verify(geocodingClient, times(1)).reverseGeocode(any(), any());
        }

        @Test
        void 게임_목록과_구역에_국가가_함께_내려온다() {
            // given
            Game game = gameRepository.save(GameFixture.WAITING_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));

            // when
            ExtractableResponse<Response> list = executeQuery(adminToken, "{ adminGames { content { id countryCode } } }");
            ExtractableResponse<Response> detail = executeQuery(
                    adminToken,
                    "{ adminGame(id: " + game.getId() + ") { area { placeName countryCode } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat((Object) list.jsonPath().get("errors")).isNull();
                softly.assertThat(list.jsonPath().getString(
                        "data.adminGames.content.find { it.id == '" + game.getId() + "' }.countryCode")).isEqualTo("KR");
                softly.assertThat(detail.jsonPath().getString("data.adminGame.area.placeName")).isEqualTo(PLACE_NAME);
                softly.assertThat(detail.jsonPath().getString("data.adminGame.area.countryCode")).isEqualTo("KR");
            });
            // placeName 과 countryCode 를 함께 물어도 같은 곳은 한 번만 조회한다
            verify(geocodingClient, times(1)).reverseGeocode(any(), any());
        }

        @Test
        void 지난_게임_기록의_구역에도_동네_이름이_내려온다() {
            // given
            Game game = gameRepository.save(GameFixture.FINISHED_GAME());
            GameResult gameResult = gameResultRepository.save(GameResultFixture.POLICE_WIN_RESULT(game.getId()));

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGameHistory(id: " + gameResult.getId() + ") { id area { placeName } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.adminGameHistory.area.placeName")).isEqualTo(PLACE_NAME);
            });
        }

        @Test
        void 주소_조회에_실패하면_동네_이름만_비우고_저장하지_않는다() {
            // given
            // 실패한 좌표는 서비스가 10분 동안 기억하므로, 다른 테스트와 겹치지 않는 좌표를 쓴다
            doReturn(GeocodingResult.failed()).when(geocodingClient).reverseGeocode(any(), any());
            Game game = gameRepository.save(GameFixture.WAITING_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game, 126.9780, 37.5665));

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminGame(id: " + game.getId() + ") { id area { playgroundRadiusInMeters placeName } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Object) response.jsonPath().get("errors")).isNull();
                softly.assertThat((Integer) response.jsonPath().get("data.adminGame.area.playgroundRadiusInMeters")).isEqualTo(500);
                softly.assertThat((Object) response.jsonPath().get("data.adminGame.area.placeName")).isNull();
                softly.assertThat(areaPlaceNameRepository.findAll()).isEmpty();
            });
        }
    }

    private ExtractableResponse<Response> executeQuery(String token, String query) {
        return authenticated(token)
                .body("{\"query\": \"" + query.replace("\"", "\\\"").replace("\n", " ") + "\"}")
                .when()
                .post(GRAPHQL_URL)
                .then()
                .extract();
    }

    private ExtractableResponse<Response> executeQuery(String query) {
        return unauthenticated()
                .body("{\"query\": \"" + query.replace("\"", "\\\"").replace("\n", " ") + "\"}")
                .when()
                .post(GRAPHQL_URL)
                .then()
                .extract();
    }
}
