package com.team.cops_and_robbers.game.area.domain;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.exception.GameAreaException;
import com.team.cops_and_robbers.game.game.domain.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;

import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.CIRCLE_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.POLYGON_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Nested
    @DisplayName("구역 축소 최종 반경 검증")
    class ValidateShrinkFinalRadius {

        private static final int PLAYGROUND_RADIUS = 500;
        private static final int JAIL_RADIUS = 50;
        private static final Integer NO_JAIL = null;

        @Test
        void null이면_구역_타입과_무관하게_통과한다() {
            // when & then
            assertThatCode(() -> gameAreaDomainService.validateCircleShrinkFinalRadius(
                    PLAYGROUND_RADIUS, JAIL_RADIUS, null)).doesNotThrowAnyException();
            assertThatCode(() -> gameAreaDomainService.validatePolygonShrinkFinalRadius(null))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(ints = {51, 200, 499})
        void 감옥_반경과_플레이그라운드_반경_사이면_통과한다(int shrinkFinalRadiusInMeters) {
            // when & then
            assertThatCode(() -> gameAreaDomainService.validateCircleShrinkFinalRadius(
                    PLAYGROUND_RADIUS, JAIL_RADIUS, shrinkFinalRadiusInMeters))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(ints = {49, 50})
        void 감옥_반경보다_작거나_같으면_거절한다(int shrinkFinalRadiusInMeters) {
            // when & then
            assertThatThrownBy(() -> gameAreaDomainService.validateCircleShrinkFinalRadius(
                    PLAYGROUND_RADIUS, JAIL_RADIUS, shrinkFinalRadiusInMeters))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameAreaException.INVALID_SHRINK_FINAL_RADIUS.getDetail());
        }

        @ParameterizedTest
        @ValueSource(ints = {500, 700})
        void 플레이그라운드_반경보다_크거나_같으면_거절한다(int shrinkFinalRadiusInMeters) {
            // when & then
            assertThatThrownBy(() -> gameAreaDomainService.validateCircleShrinkFinalRadius(
                    PLAYGROUND_RADIUS, JAIL_RADIUS, shrinkFinalRadiusInMeters))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameAreaException.INVALID_SHRINK_FINAL_RADIUS.getDetail());
        }

        @Test
        void POLYGON_구역이면_값이_있을_때_거절한다() {
            // when & then
            assertThatThrownBy(() -> gameAreaDomainService.validatePolygonShrinkFinalRadius(200))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameAreaException.SHRINK_REQUIRES_CIRCLE_AREA.getDetail());
        }

        @Test
        void 감옥이_없는_구역은_감옥_반경을_0으로_보고_통과한다() {
            // when & then
            assertThatCode(() -> gameAreaDomainService.validateCircleShrinkFinalRadius(
                    PLAYGROUND_RADIUS, NO_JAIL, 1)).doesNotThrowAnyException();
        }

        @Test
        void 저장된_CIRCLE_구역을_넘기면_반경_규칙으로_검증한다() {
            // given
            GameArea circleArea = CIRCLE_GAME_AREA(waitingGame);

            // when & then (플레이그라운드 500m, 감옥 50m)
            assertThatCode(() -> gameAreaDomainService.validateShrinkFinalRadius(circleArea, 200))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> gameAreaDomainService.validateShrinkFinalRadius(circleArea, 500))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameAreaException.INVALID_SHRINK_FINAL_RADIUS.getDetail());
        }

        @Test
        void 저장된_POLYGON_구역을_넘기면_값이_있을_때_거절한다() {
            // given
            GameArea polygonArea = POLYGON_GAME_AREA(waitingGame);

            // when & then
            assertThatCode(() -> gameAreaDomainService.validateShrinkFinalRadius(polygonArea, null))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> gameAreaDomainService.validateShrinkFinalRadius(polygonArea, 200))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameAreaException.SHRINK_REQUIRES_CIRCLE_AREA.getDetail());
        }

        @Test
        void 감옥이_없는_구역도_0이면_거절한다() {
            // when & then
            assertThatThrownBy(() -> gameAreaDomainService.validateCircleShrinkFinalRadius(
                    PLAYGROUND_RADIUS, NO_JAIL, 0))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameAreaException.INVALID_SHRINK_FINAL_RADIUS.getDetail());
        }
    }
}
