package com.team.cops_and_robbers.game.area.domain;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.game.game.domain.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.CIRCLE_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.POLYGON_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class GameAreaDomainServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameAreaDomainService gameAreaDomainService;

    private static final Long TEST_GAME_ID = 1L;
    private static final double INSIDE_LONGITUDE = 127.0276;
    private static final double INSIDE_LATITUDE = 37.4979;
    private static final double OUTSIDE_LONGITUDE = 128.0;
    private static final double OUTSIDE_LATITUDE = 38.0;

    private Game waitingGame;

    @BeforeEach
    void setUp() {
        waitingGame = WAITING_GAME();
        setId(waitingGame, TEST_GAME_ID);
    }

    @Nested
    @DisplayName("플레이그라운드 포함 여부 확인")
    class IsPointInsidePlayground {

        @Test
        void CIRCLE_타입_게임은_원형_공간_쿼리를_호출한다() {
            // given
            GameArea circleArea = CIRCLE_GAME_AREA(waitingGame);
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(circleArea);
            given(gameAreaRepository.isPointInsideCircle(TEST_GAME_ID, INSIDE_LONGITUDE, INSIDE_LATITUDE)).willReturn(true);

            // when
            boolean result = gameAreaDomainService.isPointInsidePlayground(TEST_GAME_ID, INSIDE_LONGITUDE, INSIDE_LATITUDE);

            // then
            assertThat(result).isTrue();
            then(gameAreaRepository).should().isPointInsideCircle(TEST_GAME_ID, INSIDE_LONGITUDE, INSIDE_LATITUDE);
            then(gameAreaRepository).should(never()).isPointInsidePolygon(anyLong(), anyDouble(), anyDouble());
        }

        @Test
        void CIRCLE_타입_구역_밖_좌표면_false를_반환한다() {
            // given
            GameArea circleArea = CIRCLE_GAME_AREA(waitingGame);
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(circleArea);
            given(gameAreaRepository.isPointInsideCircle(TEST_GAME_ID, OUTSIDE_LONGITUDE, OUTSIDE_LATITUDE)).willReturn(false);

            // when
            boolean result = gameAreaDomainService.isPointInsidePlayground(TEST_GAME_ID, OUTSIDE_LONGITUDE, OUTSIDE_LATITUDE);

            // then
            assertThat(result).isFalse();
            then(gameAreaRepository).should(never()).isPointInsidePolygon(anyLong(), anyDouble(), anyDouble());
        }

        @Test
        void POLYGON_타입_게임은_폴리곤_공간_쿼리를_호출한다() {
            // given
            GameArea polygonArea = POLYGON_GAME_AREA(waitingGame);
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(polygonArea);
            given(gameAreaRepository.isPointInsidePolygon(TEST_GAME_ID, INSIDE_LONGITUDE, INSIDE_LATITUDE)).willReturn(true);

            // when
            boolean result = gameAreaDomainService.isPointInsidePlayground(TEST_GAME_ID, INSIDE_LONGITUDE, INSIDE_LATITUDE);

            // then
            assertThat(result).isTrue();
            then(gameAreaRepository).should().isPointInsidePolygon(TEST_GAME_ID, INSIDE_LONGITUDE, INSIDE_LATITUDE);
            then(gameAreaRepository).should(never()).isPointInsideCircle(anyLong(), anyDouble(), anyDouble());
        }

        @Test
        void POLYGON_타입_구역_밖_좌표면_false를_반환한다() {
            // given
            GameArea polygonArea = POLYGON_GAME_AREA(waitingGame);
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(polygonArea);
            given(gameAreaRepository.isPointInsidePolygon(TEST_GAME_ID, OUTSIDE_LONGITUDE, OUTSIDE_LATITUDE)).willReturn(false);

            // when
            boolean result = gameAreaDomainService.isPointInsidePlayground(TEST_GAME_ID, OUTSIDE_LONGITUDE, OUTSIDE_LATITUDE);

            // then
            assertThat(result).isFalse();
            then(gameAreaRepository).should(never()).isPointInsideCircle(anyLong(), anyDouble(), anyDouble());
        }
    }
}