package com.team.cops_and_robbers.play.system.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;
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
    private static final String FORCE_ARREST_URL = "/api/games/{gameId}/system/force-arrest";
    private static final String ESCAPE_URL = "/api/games/{gameId}/system/escape";

    private User chaser;
    private User runner;
    private Game game;
    private GameParticipant chaserParticipant;
    private GameParticipant runnerParticipant;
    private String chaserToken;
    private String runnerToken;

    @BeforeEach
    void setUp() {
        chaser = givenUser("chaser");
        runner = givenUser("runner");

        chaserToken = givenAccessToken(chaser);
        runnerToken = givenAccessToken(runner);

        game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));
        chaserParticipant = givenChaser(game, chaser);
        runnerParticipant = givenRunner(game, runner);
    }

    @Nested
    @DisplayName("도둑 체포 API")
    class ArrestRunner {

        @Test
        void 도둑_체포_성공() {
            // given
            User secondRunner = givenUser("runner2");
            givenRunner(game, secondRunner);
            ArrestRequest request = new ArrestRequest(runnerParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
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
                softly.assertThat(result.runnerNickname()).isEqualTo(runner.getNickname());
                softly.assertThat(result.remainingRunners()).isEqualTo(1);

                GameParticipant jailedRunner = gameParticipantRepository.findById(runnerParticipant.getId()).orElseThrow();
                softly.assertThat(jailedRunner.isJailed()).isTrue();
            });
        }

        @Test
        void 모든_도둑이_체포되면_게임이_종료되어_게임과_유저는_WAITING_상태가_되어야한다() {
            // given
            ArrestRequest request = new ArrestRequest(runnerParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
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
                softly.assertThat(result.runnerNickname()).isEqualTo(runner.getNickname());
                softly.assertThat(result.remainingRunners()).isEqualTo(0);

                GameParticipant jailedRunner = gameParticipantRepository.findById(runnerParticipant.getId()).orElseThrow();
                softly.assertThat(jailedRunner.isWaiting()).isTrue();

                gameRepository.findById(game.getId()).orElseThrow();
                softly.assertThat(game.getStatus() == GameStatus.WAITING);

                softly.assertThat(gameResultRepository.existsById(1L)).isTrue();    // 게임 결과 또한 저장되어야 한다.
            });
        }

        @Test
        void 술래잡기_게임도_모든_도둑이_잡히면_전원_체포로_종료된다() {
            // given
            Game tagGame = gameRepository.save(GameFixture.IN_PROGRESS_TAG_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA_WITHOUT_JAIL(tagGame));
            User tagChaser = givenUser("tagChaser");
            User tagRunner = givenUser("tagRunner");
            givenChaser(tagGame, tagChaser);
            GameParticipant tagRunnerParticipant = givenRunner(tagGame, tagRunner);

            // when
            ExtractableResponse<Response> response = authenticated(givenAccessToken(tagChaser))
                    .body(new ArrestRequest(tagRunnerParticipant.getId()))
                    .pathParam(GAME_ID_PARAM, tagGame.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(response.as(ArrestResponse.class).remainingRunners()).isZero();
                softly.assertThat(gameRepository.findById(tagGame.getId()).orElseThrow().getStatus())
                        .isEqualTo(GameStatus.WAITING);
                softly.assertThat(gameResultRepository.findByGameId(tagGame.getId()).orElseThrow().getEndReason())
                        .isEqualTo(GameEndReason.ALL_ARRESTED);
            });
        }

        @Test
        void 게임이_진행_중이_아니면_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            givenChaser(waitingGame, chaser);
            GameParticipant waitingRunner = givenRunner(waitingGame, runner);

            ArrestRequest request = new ArrestRequest(waitingRunner.getId());

            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
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
            ArrestRequest request = new ArrestRequest(runnerParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(runnerToken)
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
            chaserParticipant.updateStatus(ParticipantStatus.CHASER_WAITING);
            gameParticipantRepository.save(chaserParticipant);

            ArrestRequest request = new ArrestRequest(runnerParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
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
            ArrestRequest request = new ArrestRequest(chaserParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
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
            runnerParticipant.updateStatus(ParticipantStatus.JAILED);
            gameParticipantRepository.save(runnerParticipant);

            ArrestRequest request = new ArrestRequest(runnerParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
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
            GameParticipant otherRunnerParticipant = givenRunner(otherGame, otherUser);

            ArrestRequest request = new ArrestRequest(otherRunnerParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(chaserToken)
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
            ArrestRequest request = new ArrestRequest(runnerParticipant.getId());

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
            ExtractableResponse<Response> response = authenticated(chaserToken)
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
    @DisplayName("도둑 강제 체포 API")
    class ForceArrestRunner {

        @Test
        void 강제_체포_성공() {
            // given
            User secondRunner = givenUser("runner2");
            givenRunner(game, secondRunner);

            // when
            ExtractableResponse<Response> response = authenticated(runnerToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(FORCE_ARREST_URL)
                    .then()
                    .extract();

            // then
            ArrestResponse result = response.as(ArrestResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.runnerNickname()).isEqualTo(runner.getNickname());
                softly.assertThat(result.remainingRunners()).isEqualTo(1);

                GameParticipant jailedRunner = gameParticipantRepository.findById(runnerParticipant.getId()).orElseThrow();
                softly.assertThat(jailedRunner.isJailed()).isTrue();
            });
        }

        @Test
        void 경찰이_대기_상태여도_강제_체포할_수_있다() {
            // given
            User secondRunner = givenUser("runner2");
            givenRunner(game, secondRunner);
            chaserParticipant.updateStatus(ParticipantStatus.CHASER_WAITING);
            gameParticipantRepository.save(chaserParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(runnerToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(FORCE_ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        }

        @Test
        void 마지막_도둑이_강제_체포되면_게임이_종료된다() {
            // when
            ExtractableResponse<Response> response = authenticated(runnerToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(FORCE_ARREST_URL)
                    .then()
                    .extract();

            // then
            ArrestResponse result = response.as(ArrestResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.remainingRunners()).isEqualTo(0);

                GameParticipant endedRunner = gameParticipantRepository.findById(runnerParticipant.getId()).orElseThrow();
                softly.assertThat(endedRunner.isWaiting()).isTrue();
                softly.assertThat(gameRepository.findById(game.getId()).orElseThrow().getStatus())
                        .isEqualTo(GameStatus.WAITING);
            });
        }

        @Test
        void 게임이_진행_중이_아니면_강제_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            User waitingRunner = givenUser("waitingRunner");
            String waitingRunnerToken = givenAccessToken(waitingRunner);
            givenRunner(waitingGame, waitingRunner);

            // when
            ExtractableResponse<Response> response = authenticated(waitingRunnerToken)
                    .pathParam(GAME_ID_PARAM, waitingGame.getId())
                    .when()
                    .post(FORCE_ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 이벤트_게임에서는_강제_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given
            game.updateEventGame(true);
            gameRepository.save(game);

            // when
            ExtractableResponse<Response> response = authenticated(runnerToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(FORCE_ARREST_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

                Game savedGame = gameRepository.findById(game.getId()).orElseThrow();
                softly.assertThat(savedGame.getTotalArrestCount()).isZero();
            });
        }

        @Test
        void 도둑이_아니면_강제_체포할_수_없으며_400_BadRequest를_응답한다() {
            // when (경찰이 요청)
            ExtractableResponse<Response> response = authenticated(chaserToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(FORCE_ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 이미_수감된_도둑은_강제_체포할_수_없으며_400_BadRequest를_응답한다() {
            // given
            runnerParticipant.updateStatus(ParticipantStatus.JAILED);
            gameParticipantRepository.save(runnerParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(runnerToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(FORCE_ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 게임_참가자가_아니면_404_NotFound를_응답한다() {
            // given
            User outsider = givenUser("outsider");
            String outsiderToken = givenAccessToken(outsider);

            // when
            ExtractableResponse<Response> response = authenticated(outsiderToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(FORCE_ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void 인증_토큰_없이_요청하면_401_Unauthorized를_응답한다() {
            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(FORCE_ARREST_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Nested
    @DisplayName("체포 개인 기록")
    class ArrestPersonalRecord {

        /** 카운트는 열려 있는 GameResult 명단에 쌓이므로, 게임 시작 시점처럼 스냅샷을 먼저 연다. */
        private GameResult openResultWithSnapshots(Game targetGame, GameParticipant... participants) {
            GameResult openedResult = gameResultRepository.save(GameResult.openSnapshot(
                    targetGame, gameAreaRepository.getByGameId(targetGame.getId()), null));
            for (GameParticipant participant : participants) {
                gameResultParticipantRepository.save(GameResultParticipant.createSnapshot(openedResult, participant));
            }
            return openedResult;
        }

        private void arrest(String token, Game targetGame, Long runnerParticipantId) {
            authenticated(token)
                    .body(new ArrestRequest(runnerParticipantId))
                    .pathParam(GAME_ID_PARAM, targetGame.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .statusCode(HttpStatus.OK.value());
        }

        @Test
        void 강제_체포는_도둑의_잡힌_횟수만_올리고_경찰의_체포_수는_올리지_않는다() {
            // given - 도둑을 하나 더 두어 전원 체포로 게임이 끝나지 않게 한다
            User secondRunner = givenUser("runner2");
            givenRunner(game, secondRunner);
            GameResult openedResult = openResultWithSnapshots(game, chaserParticipant, runnerParticipant);

            // when
            authenticated(runnerToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(FORCE_ARREST_URL)
                    .then()
                    .statusCode(HttpStatus.OK.value());

            // then
            GameResultParticipant chaserRecord = gameResultParticipantRepository
                    .findFirstByGameResultIdAndUserIdOrderByIdDesc(openedResult.getId(), chaser.getId()).orElseThrow();
            GameResultParticipant runnerRecord = gameResultParticipantRepository
                    .findFirstByGameResultIdAndUserIdOrderByIdDesc(openedResult.getId(), runner.getId()).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(runnerRecord.getArrestedCount()).isEqualTo(1);
                softly.assertThat(chaserRecord.getArrestCount()).isZero();
                softly.assertThat(gameRepository.findById(game.getId()).orElseThrow().getTotalArrestCount())
                        .isEqualTo(1);
            });
        }

        @Test
        void 체포와_탈옥_후_재체포가_경찰과_도둑의_개인_기록에_각각_누적된다() {
            // given - 도둑을 하나 더 두어 전원 체포로 게임이 끝나지 않게 한다
            User secondRunner = givenUser("runner2");
            givenRunner(game, secondRunner);
            GameResult openedResult = openResultWithSnapshots(game, chaserParticipant, runnerParticipant);

            // when - 체포 → 탈옥 → 재체포를 전부 실제 API 로 수행
            arrest(chaserToken, game, runnerParticipant.getId());
            authenticated(runnerToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ESCAPE_URL)
                    .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
            arrest(chaserToken, game, runnerParticipant.getId());

            // then
            GameResultParticipant chaserRecord = gameResultParticipantRepository
                    .findFirstByGameResultIdAndUserIdOrderByIdDesc(openedResult.getId(), chaser.getId()).orElseThrow();
            GameResultParticipant runnerRecord = gameResultParticipantRepository
                    .findFirstByGameResultIdAndUserIdOrderByIdDesc(openedResult.getId(), runner.getId()).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(chaserRecord.getArrestCount()).isEqualTo(2);
                softly.assertThat(chaserRecord.getArrestedCount()).isZero();
                softly.assertThat(runnerRecord.getArrestedCount()).isEqualTo(2);
                softly.assertThat(runnerRecord.getArrestCount()).isZero();
            });
        }

        @Test
        void 이벤트_게임에서_같은_도둑을_반복_체포하면_잡힌_횟수가_그대로_누적된다() {
            // given - 이벤트 게임은 체포돼도 JAILED 로 바뀌지 않아 탈옥 없이 바로 재체포된다
            User eventChaser = givenUser("eventChaser");
            User eventRunner = givenUser("eventRunner");
            String eventChaserToken = givenAccessToken(eventChaser);

            Game eventGame = gameRepository.save(GameFixture.EVENT_GAME());
            gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(eventGame));
            GameParticipant eventChaserParticipant = givenChaser(eventGame, eventChaser);
            GameParticipant eventRunnerParticipant = givenRunner(eventGame, eventRunner);
            GameResult openedResult = openResultWithSnapshots(eventGame, eventChaserParticipant, eventRunnerParticipant);

            // when
            arrest(eventChaserToken, eventGame, eventRunnerParticipant.getId());
            arrest(eventChaserToken, eventGame, eventRunnerParticipant.getId());

            // then
            GameResultParticipant runnerRecord = gameResultParticipantRepository
                    .findFirstByGameResultIdAndUserIdOrderByIdDesc(openedResult.getId(), eventRunner.getId()).orElseThrow();
            GameResultParticipant chaserRecord = gameResultParticipantRepository
                    .findFirstByGameResultIdAndUserIdOrderByIdDesc(openedResult.getId(), eventChaser.getId()).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(runnerRecord.getArrestedCount()).isEqualTo(2);
                softly.assertThat(chaserRecord.getArrestCount()).isEqualTo(2);
            });
        }
    }

    @Nested
    @DisplayName("폴리곤 영역 게임에서의 체포/탈옥 API")
    class WithPolygonGameArea {

        private User polygonChaser;
        private User polygonRunner;
        private Game polygonGame;
        private GameParticipant polygonChaserParticipant;
        private GameParticipant polygonRunnerParticipant;
        private String polygonChaserToken;
        private String polygonRunnerToken;

        @BeforeEach
        void setUp() {
            polygonChaser = givenUser("polygonChaser");
            polygonRunner = givenUser("polygonRunner");
            polygonChaserToken = givenAccessToken(polygonChaser);
            polygonRunnerToken = givenAccessToken(polygonRunner);

            polygonGame = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
            gameAreaRepository.save(GameAreaFixture.POLYGON_GAME_AREA(polygonGame));
            polygonChaserParticipant = givenChaser(polygonGame, polygonChaser);
            polygonRunnerParticipant = givenRunner(polygonGame, polygonRunner);
        }

        @Test
        void 폴리곤_영역에서_도둑_체포_성공() {
            // given
            User secondRunner = givenUser("polygonRunner2");
            givenRunner(polygonGame, secondRunner);
            ArrestRequest request = new ArrestRequest(polygonRunnerParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(polygonChaserToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, polygonGame.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            ArrestResponse result = response.as(ArrestResponse.class);
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                softly.assertThat(result.runnerNickname()).isEqualTo(polygonRunner.getNickname());
                softly.assertThat(result.remainingRunners()).isEqualTo(1);
            });
        }

        @Test
        void 폴리곤_영역에서_마지막_도둑이_체포되면_게임이_종료된다() {
            // given
            ArrestRequest request = new ArrestRequest(polygonRunnerParticipant.getId());

            // when
            ExtractableResponse<Response> response = authenticated(polygonChaserToken)
                    .body(request)
                    .pathParam(GAME_ID_PARAM, polygonGame.getId())
                    .when()
                    .post(ARREST_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
                Game endedGame = gameRepository.findById(polygonGame.getId()).orElseThrow();
                softly.assertThat(endedGame.isInProgress()).isFalse();
                softly.assertThat(gameResultRepository.existsById(1L)).isTrue();
            });
        }

        @Test
        void 폴리곤_영역에서_도둑_탈옥_성공() {
            // given
            polygonRunnerParticipant.updateStatus(ParticipantStatus.JAILED);
            gameParticipantRepository.save(polygonRunnerParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(polygonRunnerToken)
                    .pathParam(GAME_ID_PARAM, polygonGame.getId())
                    .when()
                    .post(ESCAPE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
            GameParticipant escapedRunner = gameParticipantRepository.findById(polygonRunnerParticipant.getId()).orElseThrow();
            assertThat(escapedRunner.isJailed()).isFalse();
        }
    }

    @Nested
    @DisplayName("도둑 탈옥 API")
    class EscapeFromPrison {

        @Test
        void 도둑_탈옥_성공() {
            // given
            runnerParticipant.updateStatus(ParticipantStatus.JAILED);
            gameParticipantRepository.save(runnerParticipant);

            // when
            ExtractableResponse<Response> response = authenticated(runnerToken)
                    .pathParam(GAME_ID_PARAM, game.getId())
                    .when()
                    .post(ESCAPE_URL)
                    .then()
                    .extract();

            // then
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

            GameParticipant escapedRunner = gameParticipantRepository.findById(runnerParticipant.getId()).orElseThrow();
            assertThat(escapedRunner.isJailed()).isFalse();
            assertThat(escapedRunner.isRunner()).isTrue();
        }

        @Test
        void 술래잡기_게임에서는_탈옥할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game tagGame = gameRepository.save(GameFixture.IN_PROGRESS_TAG_GAME());
            User tagRunner = givenUser("tagRunner");
            GameParticipant jailedRunner = givenJailedRunner(tagGame, tagRunner);

            // when
            ExtractableResponse<Response> response = authenticated(givenAccessToken(tagRunner))
                    .pathParam(GAME_ID_PARAM, tagGame.getId())
                    .when()
                    .post(ESCAPE_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                softly.assertThat(response.jsonPath().getString("errorCode")).isEqualTo("ESCAPE_NOT_ALLOWED_IN_MODE");
                softly.assertThat(gameParticipantRepository.findById(jailedRunner.getId()).orElseThrow().isJailed())
                        .isTrue();
            });
        }

        @Test
        void 게임이_진행_중이_아니면_탈옥할_수_없으며_400_BadRequest를_응답한다() {
            // given
            Game waitingGame = gameRepository.save(GameFixture.WAITING_GAME());
            GameParticipant waitingRunner = givenRunner(waitingGame, runner);
            waitingRunner.updateStatus(ParticipantStatus.JAILED);
            gameParticipantRepository.save(waitingRunner);

            // when
            ExtractableResponse<Response> response = authenticated(runnerToken)
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
            // chaserParticipant는 경찰 팀

            // when (경찰이 요청)
            ExtractableResponse<Response> response = authenticated(chaserToken)
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
            assertThat(runnerParticipant.getStatus()).isEqualTo(ParticipantStatus.ALIVE); // ALIVE 상태 확인

            // when
            ExtractableResponse<Response> response = authenticated(runnerToken)
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
            ExtractableResponse<Response> response = authenticated(runnerToken)
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
