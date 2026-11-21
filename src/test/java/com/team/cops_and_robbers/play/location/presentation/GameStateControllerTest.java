package com.team.cops_and_robbers.play.location.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.play.common.application.InGameParticipantCacheService;
import com.team.cops_and_robbers.play.location.application.RunnerLocationService;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.location.presentation.dto.GameStateResponse;
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

class GameStateControllerTest extends ControllerTest {

    private static final String STATE_URL = "/api/games/{gameId}/state";

    @Autowired
    private RunnerLocationService runnerLocationService;

    @Autowired
    private InGameParticipantCacheService inGameParticipantCacheService;

    private User chaser;
    private User runner1;
    private User runner2;
    private Game game;
    private GameParticipant chaserParticipant;
    private GameParticipant runnerParticipant1;
    private GameParticipant runnerParticipant2;
    private String chaserToken;

    @BeforeEach
    void setUp() {
        chaser = givenUser("chaser");
        runner1 = givenUser("runner1");
        runner2 = givenUser("runner2");

        chaserToken = givenAccessToken(chaser);

        game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        chaserParticipant = givenChaser(game, chaser);
        runnerParticipant1 = givenRunner(game, runner1);
        runnerParticipant2 = givenRunner(game, runner2);

        inGameParticipantCacheService.clearCache(game.getId());
        inGameParticipantCacheService.loadCache(game.getId());
        runnerLocationService.clearRunnerLocations(game.getId());
        runnerLocationService.clearRunnerLocationSnapshot(game.getId());
    }

    @Nested
    @DisplayName("게임 상태 조회 API")
    class GetGameState {

        @Test
        void 스냅샷과_전체_참여자_현황을_함께_반환한다() {
            // given
            runnerLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), runnerParticipant1.getId(), 37.5665, 126.9780)
            );
            runnerLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), runnerParticipant2.getId(), 37.1234, 126.1234)
            );
            runnerLocationService.revealRunnerLocations(game.getId());

            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
                    .when()
                    .get(STATE_URL, game.getId())
                    .then()
                    .extract();

            // then
            GameStateResponse result = response.as(GameStateResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(result.gameMode()).isEqualTo(GameMode.COPS_AND_ROBBERS);
                softly.assertThat(result.runnerLocations()).hasSize(2);
                softly.assertThat(result.participants()).hasSize(3);
            });
        }

        @Test
        void 잡힌_도둑의_위치는_응답에_포함되지_않는다() {
            // given: 두 도둑 모두 위치 업데이트, runner2를 JAILED로 변경 후 공개
            runnerLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), runnerParticipant1.getId(), 37.5665, 126.9780)
            );
            runnerLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), runnerParticipant2.getId(), 37.1111, 126.1111)
            );
            runnerParticipant2.updateStatus(ParticipantStatus.JAILED);
            gameParticipantRepository.save(runnerParticipant2);
            runnerLocationService.revealRunnerLocations(game.getId());

            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
                    .when()
                    .get(STATE_URL, game.getId())
                    .then()
                    .extract();

            // then: 생존 도둑만 위치 반환, 참여자 현황에는 3명 모두 포함
            GameStateResponse result = response.as(GameStateResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(result.runnerLocations()).hasSize(1);
                softly.assertThat(result.runnerLocations().get(0).participantId()).isEqualTo(runnerParticipant1.getId());
                softly.assertThat(result.participants()).hasSize(3);
            });
        }

        @Test
        void 경찰_대기시간_중에는_경찰과_도둑_모두_위치가_빈_목록으로_반환된다() {
            // given: 스냅샷이 존재하더라도 경찰이 한 명이라도 CHASER_WAITING이면 빈 배열
            runnerLocationService.updateLocation(
                    new LocationUpdateCommand(game.getId(), runnerParticipant1.getId(), 37.5665, 126.9780)
            );
            runnerLocationService.revealRunnerLocations(game.getId());
            givenWaitingChaser(game, chaser);

            String runner1Token = givenAccessToken(runner1);

            // when: 경찰 요청
            ExtractableResponse<Response> chaserResponse = authenticated(chaserToken)
                    .when()
                    .get(STATE_URL, game.getId())
                    .then()
                    .extract();

            // when: 도둑 요청
            ExtractableResponse<Response> runnerResponse = authenticated(runner1Token)
                    .when()
                    .get(STATE_URL, game.getId())
                    .then()
                    .extract();

            // then: 요청자가 누구든 빈 배열
            assertSoftly(softly -> {
                softly.assertThat(chaserResponse.statusCode()).isEqualTo(200);
                softly.assertThat(chaserResponse.as(GameStateResponse.class).runnerLocations()).isEmpty();
                softly.assertThat(runnerResponse.statusCode()).isEqualTo(200);
                softly.assertThat(runnerResponse.as(GameStateResponse.class).runnerLocations()).isEmpty();
            });
        }

        @Test
        void 게임이_존재하지_않으면_404_NotFound를_응답한다() {
            ExtractableResponse<Response> response = authenticated(chaserToken)
                    .when()
                    .get(STATE_URL, game.getId() + 999)
                    .then()
                    .extract();

            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void 해당_게임의_참가자가_아니면_404_NotFound를_응답한다() {
            User outsider = givenUser("outsider");
            String outsiderToken = givenAccessToken(outsider);

            ExtractableResponse<Response> response = authenticated(outsiderToken)
                    .when()
                    .get(STATE_URL, game.getId())
                    .then()
                    .extract();

            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void 게임이_진행_중이_아니면_400_BadRequest를_응답한다() {
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            givenChaser(waitingGame, chaser);

            ExtractableResponse<Response> response = authenticated(chaserToken)
                    .when()
                    .get(STATE_URL, waitingGame.getId())
                    .then()
                    .extract();

            assertThat(response.statusCode()).isEqualTo(400);
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            ExtractableResponse<Response> response = unauthenticated()
                    .when()
                    .get(STATE_URL, game.getId())
                    .then()
                    .extract();

            assertThat(response.statusCode()).isEqualTo(401);
        }
    }
}
