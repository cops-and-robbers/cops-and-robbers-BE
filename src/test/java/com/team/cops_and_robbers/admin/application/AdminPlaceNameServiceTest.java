package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult.AdminCoordinateResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameSummaryResult;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.community.post.domain.PostAddress;
import com.team.cops_and_robbers.community.post.infrastructure.GeocodingResult;
import com.team.cops_and_robbers.game.area.domain.AreaPlaceName;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.game.domain.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminPlaceNameServiceTest extends ServiceUnitTest {

    private static final String GANGNAM_PLACE_NAME = "서울특별시 강남구 역삼동";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-11-10T03:00:00Z");

    @InjectMocks
    private AdminPlaceNameService adminPlaceNameService;

    @Mock
    private AdminGameService adminGameService;

    @Spy
    private Clock clock = Clock.fixed(NOW, KST);

    private Game game;

    @BeforeEach
    void setUp() {
        game = GameFixture.WAITING_GAME();
        setId(game, 1L);
    }

    @Nested
    @DisplayName("구역별 동네 이름 조회")
    class GetPlaceNamesByArea {

        @Test
        void 저장된_동네_이름은_주소를_조회하지_않고_반환한다() {
            // given
            AdminGameAreaResult area = circleArea(127.0276, 37.4979);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection()))
                    .willReturn(List.of(AreaPlaceName.create("37.498,127.028", GANGNAM_PLACE_NAME)));

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            assertThat(result).containsEntry(area, GANGNAM_PLACE_NAME);
            verifyNoInteractions(geocodingClient);
            verify(areaPlaceNameRepository, never()).insertIfAbsent(anyString(), anyString(), any());
        }

        @Test
        void 저장되지_않은_곳은_반올림한_좌표로_주소를_조회해_저장한다() {
            // given
            AdminGameAreaResult area = circleArea(127.0276, 37.4979);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(37.498, 127.028)).willReturn(resolved(GANGNAM_PLACE_NAME));

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            assertThat(result).containsEntry(area, GANGNAM_PLACE_NAME);
            verify(areaPlaceNameRepository).insertIfAbsent(eq("37.498,127.028"), eq(GANGNAM_PLACE_NAME), any());
        }

        @Test
        void 약_100m_안의_구역은_한_번만_조회해_같은_이름을_쓴다() {
            // given
            AdminGameAreaResult area = circleArea(127.0276, 37.4979);
            AdminGameAreaResult nearbyArea = circleArea(127.0279, 37.4981);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(37.498, 127.028)).willReturn(resolved(GANGNAM_PLACE_NAME));

            // when
            Map<AdminGameAreaResult, String> result =
                    adminPlaceNameService.getPlaceNamesByArea(List.of(area, nearbyArea));

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).containsEntry(area, GANGNAM_PLACE_NAME);
                softly.assertThat(result).containsEntry(nearbyArea, GANGNAM_PLACE_NAME);
            });
            verify(geocodingClient, times(1)).reverseGeocode(anyDouble(), anyDouble());
        }

        @Test
        void 다각형_구역은_무게중심으로_조회한다() {
            // given
            AdminGameAreaResult area = AdminGameAreaResult.from(GameAreaFixture.POLYGON_GAME_AREA(game));
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(37.499, 127.029)).willReturn(resolved(GANGNAM_PLACE_NAME));

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            assertThat(result).containsEntry(area, GANGNAM_PLACE_NAME);
        }

        @Test
        void 새로_조회하는_곳은_한_번에_5곳까지다() {
            // given
            List<AdminGameAreaResult> areas = IntStream.range(0, 7)
                    .mapToObj(i -> circleArea(127.0, 37.40 + i * 0.01))
                    .toList();
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(anyDouble(), anyDouble())).willReturn(resolved(GANGNAM_PLACE_NAME));

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(areas);

            // then
            assertThat(result).hasSize(5);
            verify(geocodingClient, times(5)).reverseGeocode(anyDouble(), anyDouble());
        }

        @Test
        void 주소_조회에_실패하면_이름_없이_반환하고_저장하지_않는다() {
            // given
            AdminGameAreaResult area = circleArea(127.0276, 37.4979);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(anyDouble(), anyDouble())).willReturn(GeocodingResult.failed());

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            assertThat(result).isEmpty();
            verify(areaPlaceNameRepository, never()).insertIfAbsent(anyString(), anyString(), any());
        }

        @Test
        void 주소가_없는_곳은_이름_없이_반환하고_저장하지_않는다() {
            // given
            AdminGameAreaResult area = circleArea(127.0276, 37.4979);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(anyDouble(), anyDouble())).willReturn(GeocodingResult.notFound());

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            assertThat(result).isEmpty();
            verify(areaPlaceNameRepository, never()).insertIfAbsent(anyString(), anyString(), any());
        }

        @Test
        void 주소_조회_중_예외가_나도_어드민_조회는_실패하지_않는다() {
            // given
            AdminGameAreaResult area = circleArea(127.0276, 37.4979);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(anyDouble(), anyDouble())).willThrow(new IllegalStateException());

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void 저장에_실패해도_찾은_이름을_반환한다() {
            // given
            AdminGameAreaResult area = circleArea(127.0276, 37.4979);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(anyDouble(), anyDouble())).willReturn(resolved(GANGNAM_PLACE_NAME));
            doThrow(new DataAccessResourceFailureException("db down"))
                    .when(areaPlaceNameRepository).insertIfAbsent(anyString(), anyString(), any());

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            assertThat(result).containsEntry(area, GANGNAM_PLACE_NAME);
        }

        @Test
        void 저장된_이름을_읽다가_실패해도_어드민_조회는_실패하지_않는다() {
            // given
            AdminGameAreaResult area = circleArea(127.0276, 37.4979);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection()))
                    .willThrow(new DataAccessResourceFailureException("db down"));

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void 조회에_실패한_곳은_10분_동안_다시_조회하지_않는다() {
            // given
            AdminGameAreaResult area = circleArea(127.0276, 37.4979);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(anyDouble(), anyDouble())).willReturn(GeocodingResult.failed());
            adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // when
            doReturn(NOW.plus(Duration.ofMinutes(9))).when(clock).instant();
            adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            verify(geocodingClient, times(1)).reverseGeocode(anyDouble(), anyDouble());
        }

        @Test
        void 실패한_곳도_10분이_지나면_다시_조회한다() {
            // given
            AdminGameAreaResult area = circleArea(127.0276, 37.4979);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(anyDouble(), anyDouble()))
                    .willReturn(GeocodingResult.failed())
                    .willReturn(resolved(GANGNAM_PLACE_NAME));
            adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // when
            doReturn(NOW.plus(Duration.ofMinutes(10))).when(clock).instant();
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            assertThat(result).containsEntry(area, GANGNAM_PLACE_NAME);
            verify(geocodingClient, times(2)).reverseGeocode(anyDouble(), anyDouble());
        }

        @Test
        void 실패한_곳은_건너뛰고_다음_곳을_조회한다() {
            // given
            List<AdminGameAreaResult> notFoundAreas = IntStream.range(0, 5)
                    .mapToObj(i -> circleArea(127.0, 37.40 + i * 0.01))
                    .toList();
            AdminGameAreaResult nextArea = circleArea(127.0276, 37.4979);
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(anyDouble(), anyDouble())).willReturn(GeocodingResult.notFound());
            given(geocodingClient.reverseGeocode(37.498, 127.028)).willReturn(resolved(GANGNAM_PLACE_NAME));
            adminPlaceNameService.getPlaceNamesByArea(notFoundAreas);

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(
                    Stream.concat(notFoundAreas.stream(), Stream.of(nextArea)).toList());

            // then
            assertThat(result).containsOnlyKeys(nextArea);
        }

        @Test
        void 오목한_다각형은_구역_안의_지점으로_조회한다() {
            // given
            // ㄷ자 구역이라 무게중심(127.015, 37.514)이 파인 곳에 떨어진다
            AdminGameAreaResult area = polygonArea(new double[][]{
                    {127.000, 37.500}, {127.030, 37.500}, {127.030, 37.530}, {127.020, 37.530},
                    {127.020, 37.510}, {127.010, 37.510}, {127.010, 37.530}, {127.000, 37.530}
            });
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection())).willReturn(List.of());
            given(geocodingClient.reverseGeocode(anyDouble(), anyDouble())).willReturn(resolved(GANGNAM_PLACE_NAME));

            // when
            adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            ArgumentCaptor<Double> latitude = ArgumentCaptor.forClass(Double.class);
            ArgumentCaptor<Double> longitude = ArgumentCaptor.forClass(Double.class);
            verify(geocodingClient).reverseGeocode(latitude.capture(), longitude.capture());
            boolean insideNotch = longitude.getValue() > 127.010 && longitude.getValue() < 127.020
                    && latitude.getValue() > 37.510;
            assertThat(insideNotch).isFalse();
        }

        @Test
        void 좌표가_없는_기록은_조회하지_않는다() {
            // given
            AdminGameAreaResult area = new AdminGameAreaResult(
                    AreaType.CIRCLE, null, null, null, null, null, null, null, null);

            // when
            Map<AdminGameAreaResult, String> result = adminPlaceNameService.getPlaceNamesByArea(List.of(area));

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(geocodingClient, areaPlaceNameRepository);
        }
    }

    @Nested
    @DisplayName("게임 목록의 동네 이름 조회")
    class GetPlaceNamesByGame {

        @Test
        void 구역이_있는_게임만_동네_이름이_담긴다() {
            // given
            Game gameWithoutArea = GameFixture.WAITING_GAME();
            setId(gameWithoutArea, 2L);
            AdminGameSummaryResult summary = AdminGameSummaryResult.from(game, 0);
            AdminGameSummaryResult summaryWithoutArea = AdminGameSummaryResult.from(gameWithoutArea, 0);

            given(adminGameService.getAreasByGameSummary(List.of(summary, summaryWithoutArea)))
                    .willReturn(Map.of(summary, circleArea(127.0276, 37.4979)));
            given(areaPlaceNameRepository.findByCoordinateKeyIn(anyCollection()))
                    .willReturn(List.of(AreaPlaceName.create("37.498,127.028", GANGNAM_PLACE_NAME)));

            // when
            Map<AdminGameSummaryResult, String> result =
                    adminPlaceNameService.getPlaceNamesByGame(List.of(summary, summaryWithoutArea));

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).containsEntry(summary, GANGNAM_PLACE_NAME);
                softly.assertThat(result).doesNotContainKey(summaryWithoutArea);
            });
        }
    }

    private AdminGameAreaResult circleArea(double longitude, double latitude) {
        return AdminGameAreaResult.from(GameAreaFixture.CIRCLE_GAME_AREA(game, longitude, latitude));
    }

    private AdminGameAreaResult polygonArea(double[][] vertices) {
        List<AdminCoordinateResult> coordinates = Arrays.stream(vertices)
                .map(vertex -> new AdminCoordinateResult(vertex[1], vertex[0]))
                .toList();
        return new AdminGameAreaResult(AreaType.POLYGON, null, null, null, null, null, null, coordinates, null);
    }

    private GeocodingResult resolved(String region) {
        return GeocodingResult.resolved(PostAddress.of(null, null, null, region, "KR"));
    }
}
