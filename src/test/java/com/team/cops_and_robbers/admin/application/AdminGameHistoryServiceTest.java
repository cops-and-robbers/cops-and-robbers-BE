package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.game.AdminGameHistoryListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameHistoryPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameHistoryParticipantResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameHistoryResult;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.fixture.GameResultFixture;
import com.team.cops_and_robbers.common.fixture.GameResultParticipantFixture;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import com.team.cops_and_robbers.history.exception.GameResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.isNull;

class AdminGameHistoryServiceTest extends ServiceUnitTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 1, 1, 0, 0);

    @InjectMocks
    private AdminGameHistoryService adminGameHistoryService;

    private GameResult createGameResult(Long gameId, Long id) {
        return prepare(GameResultFixture.POLICE_WIN_RESULT(gameId), id);
    }

    private GameResult prepare(GameResult gameResult, Long id) {
        setId(gameResult, id);
        org.springframework.test.util.ReflectionTestUtils.setField(gameResult, "createdAt", FIXED_TIME);
        return gameResult;
    }

    @Nested
    @DisplayName("게임 히스토리 목록 조회")
    class GetGameHistoryList {

        @Test
        void 필터_없이_히스토리_목록을_조회하면_전체_페이지를_반환한다() {
            // given
            AdminGameHistoryListCommand command = new AdminGameHistoryListCommand(
                    0, 10, null, SortDirection.DESC);

            GameResult result1 = createGameResult(1L, 10L);
            GameResult result2 = createGameResult(2L, 11L);

            Page<GameResult> resultPage = new PageImpl<>(
                    List.of(result1, result2), PageRequest.of(0, 10), 2);
            given(gameResultRepository.findAllForAdmin(isNull(), any())).willReturn(resultPage);

            // when
            AdminGameHistoryPageResult result = adminGameHistoryService.getGameHistoryList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(2);
                softly.assertThat(result.content()).hasSize(2);
            });
        }

        @Test
        void 종료_사유_필터로_히스토리를_조회하면_해당_사유만_반환한다() {
            // given
            AdminGameHistoryListCommand command = new AdminGameHistoryListCommand(
                    0, 10, GameEndReason.ALL_ARRESTED, SortDirection.DESC);

            GameResult result1 = createGameResult(1L, 10L);

            Page<GameResult> resultPage = new PageImpl<>(
                    List.of(result1), PageRequest.of(0, 10), 1);
            given(gameResultRepository.findAllForAdmin(any(GameEndReason.class), any()))
                    .willReturn(resultPage);

            // when
            AdminGameHistoryPageResult result = adminGameHistoryService.getGameHistoryList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(1);
                softly.assertThat(result.content().get(0).endReason())
                        .isEqualTo(GameEndReason.ALL_ARRESTED);
            });
        }

        @Test
        void 원형_구역_기록이면_area에_중심_좌표와_반경이_매핑된다() {
            // given
            AdminGameHistoryListCommand command = new AdminGameHistoryListCommand(
                    0, 10, null, SortDirection.DESC);

            GameResult circleResult = createGameResult(1L, 10L);
            Page<GameResult> resultPage = new PageImpl<>(
                    List.of(circleResult), PageRequest.of(0, 10), 1);
            given(gameResultRepository.findAllForAdmin(isNull(), any())).willReturn(resultPage);

            // when
            AdminGameHistoryPageResult result = adminGameHistoryService.getGameHistoryList(command);

            // then
            AdminGameAreaResult area = result.content().get(0).area();
            assertSoftly(softly -> {
                softly.assertThat(area.areaType()).isEqualTo(AreaType.CIRCLE);
                softly.assertThat(area.playgroundCenterLat()).isEqualTo(37.4979);
                softly.assertThat(area.playgroundCenterLng()).isEqualTo(127.0276);
                softly.assertThat(area.playgroundRadiusInMeters()).isEqualTo(500);
                softly.assertThat(area.jailCenterLat()).isEqualTo(37.4989);
                softly.assertThat(area.jailCenterLng()).isEqualTo(127.0286);
                softly.assertThat(area.jailRadiusInMeters()).isEqualTo(50);
                softly.assertThat(area.playgroundPolygon()).isNull();
            });
        }

        @Test
        void 다각형_구역_기록이면_area에_폴리곤_좌표가_매핑된다() {
            // given
            AdminGameHistoryListCommand command = new AdminGameHistoryListCommand(
                    0, 10, null, SortDirection.DESC);

            GameResult polygonResult = prepare(GameResultFixture.POLICE_WIN_RESULT_POLYGON(1L), 10L);
            Page<GameResult> resultPage = new PageImpl<>(
                    List.of(polygonResult), PageRequest.of(0, 10), 1);
            given(gameResultRepository.findAllForAdmin(isNull(), any())).willReturn(resultPage);

            // when
            AdminGameHistoryPageResult result = adminGameHistoryService.getGameHistoryList(command);

            // then
            AdminGameAreaResult area = result.content().get(0).area();
            assertSoftly(softly -> {
                softly.assertThat(area.areaType()).isEqualTo(AreaType.POLYGON);
                softly.assertThat(area.playgroundPolygon()).hasSize(4);
                softly.assertThat(area.playgroundPolygon().get(0).latitude()).isEqualTo(37.4979);
                softly.assertThat(area.playgroundPolygon().get(0).longitude()).isEqualTo(127.0276);
                softly.assertThat(area.playgroundCenterLat()).isNull();
                softly.assertThat(area.jailPolygon()).hasSize(4);
                softly.assertThat(area.jailPolygon().get(0).latitude()).isEqualTo(37.4983);
                softly.assertThat(area.jailPolygon().get(0).longitude()).isEqualTo(127.0280);
            });
        }

        @Test
        void 감옥_좌표가_없는_기록은_jail_필드만_null이다() {
            // given
            AdminGameHistoryListCommand command = new AdminGameHistoryListCommand(
                    0, 10, null, SortDirection.DESC);

            GameResult withoutJailResult = prepare(GameResultFixture.POLICE_WIN_RESULT_WITHOUT_JAIL(1L), 10L);
            Page<GameResult> resultPage = new PageImpl<>(
                    List.of(withoutJailResult), PageRequest.of(0, 10), 1);
            given(gameResultRepository.findAllForAdmin(isNull(), any())).willReturn(resultPage);

            // when
            AdminGameHistoryPageResult result = adminGameHistoryService.getGameHistoryList(command);

            // then
            AdminGameAreaResult area = result.content().get(0).area();
            assertSoftly(softly -> {
                softly.assertThat(area.playgroundCenterLat()).isEqualTo(37.4979);
                softly.assertThat(area.jailCenterLat()).isNull();
                softly.assertThat(area.jailCenterLng()).isNull();
                softly.assertThat(area.jailRadiusInMeters()).isNull();
            });
        }

        @Test
        void 좌표가_없는_과거_기록이면_area가_null이다() {
            // given
            AdminGameHistoryListCommand command = new AdminGameHistoryListCommand(
                    0, 10, null, SortDirection.DESC);

            GameResult legacyResult = prepare(GameResultFixture.LEGACY_RESULT_WITHOUT_AREA(1L), 10L);
            Page<GameResult> resultPage = new PageImpl<>(
                    List.of(legacyResult), PageRequest.of(0, 10), 1);
            given(gameResultRepository.findAllForAdmin(isNull(), any())).willReturn(resultPage);

            // when
            AdminGameHistoryPageResult result = adminGameHistoryService.getGameHistoryList(command);

            // then
            assertThat(result.content().get(0).area()).isNull();
        }

        @Test
        void 결과가_없으면_빈_페이지를_반환한다() {
            // given
            AdminGameHistoryListCommand command = new AdminGameHistoryListCommand(
                    0, 10, null, SortDirection.ASC);

            Page<GameResult> emptyPage = new PageImpl<>(
                    List.of(), PageRequest.of(0, 10), 0);
            given(gameResultRepository.findAllForAdmin(isNull(), any())).willReturn(emptyPage);

            // when
            AdminGameHistoryPageResult result = adminGameHistoryService.getGameHistoryList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isZero();
                softly.assertThat(result.content()).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("게임 히스토리 단건 조회")
    class GetGameHistory {

        @Test
        void 히스토리_단건을_조회하면_구역_정보를_포함해_반환한다() {
            // given
            GameResult gameResult = createGameResult(1L, 10L);
            given(gameResultRepository.getByGameResultId(10L)).willReturn(gameResult);

            // when
            AdminGameHistoryResult result = adminGameHistoryService.getGameHistory(10L);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.id()).isEqualTo(10L);
                softly.assertThat(result.gameId()).isEqualTo(1L);
                softly.assertThat(result.area()).isNotNull();
            });
        }

        @Test
        void 존재하지_않는_히스토리를_조회하면_GAME_RESULT_NOT_FOUND_예외가_발생한다() {
            // given
            given(gameResultRepository.getByGameResultId(999L))
                    .willThrow(new ApplicationException(GameResultException.GAME_RESULT_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> adminGameHistoryService.getGameHistory(999L))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameResultException.GAME_RESULT_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("히스토리별 참여자 배치 조회")
    class GetHistoryParticipantsByGame {

        @Test
        void 참여자가_있는_히스토리의_참여자_목록을_반환한다() {
            // given
            GameResult gameResult1 = createGameResult(1L, 10L);
            GameResult gameResult2 = createGameResult(2L, 11L);

            GameResultParticipant participant =
                    GameResultParticipantFixture.POLICE_PARTICIPANT(gameResult1, 100L, "player1");
            setId(participant, 1000L);

            AdminGameHistoryResult history1 = AdminGameHistoryResult.from(gameResult1);
            AdminGameHistoryResult history2 = AdminGameHistoryResult.from(gameResult2);
            List<AdminGameHistoryResult> histories = List.of(history1, history2);

            given(gameResultParticipantRepository.findByGameResultIdIn(List.of(10L, 11L)))
                    .willReturn(List.of(participant));

            // when
            Map<AdminGameHistoryResult, List<AdminGameHistoryParticipantResult>> result =
                    adminGameHistoryService.getHistoryParticipantsByGame(histories);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(2);
                softly.assertThat(result.get(history1)).hasSize(1);
                softly.assertThat(result.get(history1).get(0).nickname()).isEqualTo("player1");
                softly.assertThat(result.get(history1).get(0).team()).isEqualTo(Team.POLICE);
                softly.assertThat(result.get(history2)).isEmpty();
            });
        }

        @Test
        void 참여자가_없는_히스토리는_빈_리스트를_반환한다() {
            // given
            GameResult gameResult = createGameResult(1L, 10L);

            AdminGameHistoryResult history = AdminGameHistoryResult.from(gameResult);
            given(gameResultParticipantRepository.findByGameResultIdIn(anyList()))
                    .willReturn(List.of());

            // when
            Map<AdminGameHistoryResult, List<AdminGameHistoryParticipantResult>> result =
                    adminGameHistoryService.getHistoryParticipantsByGame(List.of(history));

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.get(history)).isEmpty();
            });
        }

        @Test
        void 한_히스토리에_여러_참여자가_있는_경우_모두_반환한다() {
            // given
            GameResult gameResult = createGameResult(1L, 10L);

            GameResultParticipant participant1 =
                    GameResultParticipantFixture.POLICE_PARTICIPANT(gameResult, 100L, "police1");
            setId(participant1, 1000L);
            GameResultParticipant participant2 =
                    GameResultParticipantFixture.ROBBER_PARTICIPANT(gameResult, 101L, "robber1");
            setId(participant2, 1001L);

            AdminGameHistoryResult history = AdminGameHistoryResult.from(gameResult);
            given(gameResultParticipantRepository.findByGameResultIdIn(List.of(10L)))
                    .willReturn(List.of(participant1, participant2));

            // when
            Map<AdminGameHistoryResult, List<AdminGameHistoryParticipantResult>> result =
                    adminGameHistoryService.getHistoryParticipantsByGame(List.of(history));

            // then
            assertThat(result.get(history)).hasSize(2);
        }
    }
}