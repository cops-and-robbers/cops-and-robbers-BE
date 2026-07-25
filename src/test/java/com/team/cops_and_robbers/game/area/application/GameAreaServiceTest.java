package com.team.cops_and_robbers.game.area.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.area.application.dto.command.GameAreaCommand;
import com.team.cops_and_robbers.game.area.application.dto.result.GameAreaResult;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.domain.GameAreaDomainService;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.CIRCLE_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.POLYGON_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameFixture.FINISHED_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.HOST_PARTICIPANT;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class GameAreaServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameAreaService gameAreaService;

    @Mock
    private GameAreaDomainService gameAreaDomainService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_GAME_ID = 1L;

    private User user;
    private Game waitingGame;
    private Game inProgressGame;
    private Game finishedGame;
    private GameParticipant participant;
    private GameArea gameArea;

    @BeforeEach
    void setUp() {
        user = USER();
        waitingGame = WAITING_GAME();
        inProgressGame = IN_PROGRESS_GAME();
        finishedGame = FINISHED_GAME();
        setId(user, TEST_USER_ID);
        setId(waitingGame, TEST_GAME_ID);
        setId(inProgressGame, TEST_GAME_ID);
        setId(finishedGame, TEST_GAME_ID);
        participant = HOST_PARTICIPANT(waitingGame, user);
        gameArea = CIRCLE_GAME_AREA(waitingGame);
    }

    @Nested
    @DisplayName("맵 정보 조회")
    class GetGameArea {

        @Test
        void WAITING_상태_게임의_맵_정보_조회에_성공한다() {
            // given
            GameAreaCommand command = GameAreaCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID)).willReturn(participant);
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(gameArea);

            // when
            GameAreaResult result = gameAreaService.getGameArea(command);

            // then
            assertThat(result.areaData()).isInstanceOfSatisfying(GameAreaData.CircleAreaData.class, data -> {
                assertThat(data.playgroundLatitude()).isEqualTo(gameArea.getPlaygroundCenter().getY());
                assertThat(data.playgroundLongitude()).isEqualTo(gameArea.getPlaygroundCenter().getX());
                assertThat(data.playgroundRadiusInMeters()).isEqualTo(gameArea.getPlaygroundRadiusInMeters());
                assertThat(data.jailLatitude()).isEqualTo(gameArea.getJailCenter().getY());
                assertThat(data.jailLongitude()).isEqualTo(gameArea.getJailCenter().getX());
                assertThat(data.jailRadiusInMeters()).isEqualTo(gameArea.getJailRadiusInMeters());
            });
        }

        @Test
        void IN_PROGRESS_상태_게임의_맵_정보_조회에_성공한다() {
            // given
            GameAreaCommand command = GameAreaCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(inProgressGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID)).willReturn(participant);
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(gameArea);

            // when
            GameAreaResult result = gameAreaService.getGameArea(command);

            // then
            assertThat(result.areaData()).isInstanceOfSatisfying(GameAreaData.CircleAreaData.class, data -> {
                assertThat(data.playgroundRadiusInMeters()).isEqualTo(gameArea.getPlaygroundRadiusInMeters());
                assertThat(data.jailRadiusInMeters()).isEqualTo(gameArea.getJailRadiusInMeters());
            });
        }

        @Test
        void POLYGON_타입_게임의_맵_정보_조회에_성공한다() {
            // given
            GameAreaCommand command = GameAreaCommand.of(TEST_USER_ID, TEST_GAME_ID);
            GameArea polygonGameArea = POLYGON_GAME_AREA(waitingGame);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID)).willReturn(participant);
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(polygonGameArea);

            // when
            GameAreaResult result = gameAreaService.getGameArea(command);

            // then
            assertThat(result.areaData()).isInstanceOfSatisfying(GameAreaData.PolygonAreaData.class, data -> {
                assertThat(data.playgroundPolygon()).hasSize(4);
                assertThat(data.jailPolygon()).hasSize(4);
            });
        }

        @Test
        void 게임이_비활성_상태이면_예외가_발생한다() {
            // given
            GameAreaCommand command = GameAreaCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(finishedGame);

            // when & then
            assertThatThrownBy(() -> gameAreaService.getGameArea(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameException.GAME_NOT_ACTIVE.getDetail());
        }

        @Test
        void 해당_게임의_참가자가_아니면_예외가_발생한다() {
            // given
            GameAreaCommand command = GameAreaCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID))
                    .willThrow(new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> gameAreaService.getGameArea(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.PARTICIPANT_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("GameArea 생성")
    class BuildGameArea {

        @Test
        void CIRCLE_타입_GameArea_생성에_성공한다() {
            // given
            GameAreaData.CircleAreaData circleData = new GameAreaData.CircleAreaData(
                    37.4979, 127.0276, 1000,
                    37.4983, 127.0280, 100
            );

            // when
            GameArea result = gameAreaService.buildGameArea(waitingGame, circleData);

            // then
            assertThat(result.getAreaType()).isEqualTo(AreaType.CIRCLE);
            assertThat(result.getPlaygroundRadiusInMeters()).isEqualTo(1000);
            assertThat(result.getJailRadiusInMeters()).isEqualTo(100);
            then(gameAreaDomainService).should().validateCircleAreaContainment(
                    anyDouble(), anyDouble(), anyInt(), anyDouble(), anyDouble(), anyInt()
            );
        }

        @Test
        void POLYGON_타입_GameArea_생성에_성공한다() {
            // given
            GameAreaData.PolygonAreaData polygonData = createPolygonAreaData();

            // when
            GameArea result = gameAreaService.buildGameArea(waitingGame, polygonData);

            // then
            assertThat(result.getAreaType()).isEqualTo(AreaType.POLYGON);
            assertThat(result.getPlaygroundPolygon()).isNotNull();
            assertThat(result.getJailPolygon()).isNotNull();
            then(gameAreaDomainService).should().validatePolygonAreaContainment(
                    any(Polygon.class), any(Polygon.class)
            );
        }
    }

    @Nested
    @DisplayName("GameArea 수정")
    class ApplyAreaUpdate {

        @Test
        void CIRCLE_타입으로_GameArea를_수정한다() {
            // given
            GameArea polygonArea = POLYGON_GAME_AREA(waitingGame);
            GameAreaData.CircleAreaData circleData = new GameAreaData.CircleAreaData(
                    37.4979, 127.0276, 1000,
                    37.4983, 127.0280, 100
            );

            // when
            gameAreaService.applyAreaUpdate(polygonArea, circleData);

            // then
            assertThat(polygonArea.getAreaType()).isEqualTo(AreaType.CIRCLE);
            assertThat(polygonArea.getPlaygroundPolygon()).isNull();
            then(gameAreaDomainService).should().validateCircleAreaContainment(
                    anyDouble(), anyDouble(), anyInt(), anyDouble(), anyDouble(), anyInt()
            );
        }

        @Test
        void POLYGON_타입으로_GameArea를_수정한다() {
            // given
            GameArea circleArea = CIRCLE_GAME_AREA(waitingGame);
            GameAreaData.PolygonAreaData polygonData = createPolygonAreaData();

            // when
            gameAreaService.applyAreaUpdate(circleArea, polygonData);

            // then
            assertThat(circleArea.getAreaType()).isEqualTo(AreaType.POLYGON);
            assertThat(circleArea.getPlaygroundCenter()).isNull();
            then(gameAreaDomainService).should().validatePolygonAreaContainment(
                    any(Polygon.class), any(Polygon.class)
            );
        }
    }

    private GameAreaData.PolygonAreaData createPolygonAreaData() {
        List<Coordinates> playgroundPolygon = List.of(
                new Coordinates(37.4979, 127.0276),
                new Coordinates(37.4979, 127.0296),
                new Coordinates(37.4999, 127.0296),
                new Coordinates(37.4999, 127.0276)
        );
        List<Coordinates> jailPolygon = List.of(
                new Coordinates(37.4983, 127.0280),
                new Coordinates(37.4983, 127.0285),
                new Coordinates(37.4988, 127.0285),
                new Coordinates(37.4988, 127.0280)
        );
        return new GameAreaData.PolygonAreaData(playgroundPolygon, jailPolygon);
    }
}