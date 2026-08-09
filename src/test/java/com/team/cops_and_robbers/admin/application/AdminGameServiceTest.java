package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.game.AdminGameListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameDetailResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGamePageResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminParticipantResult;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.common.fixture.GameResultFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantCountProjection;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.isNull;

class AdminGameServiceTest extends ServiceUnitTest {

    @InjectMocks
    private AdminGameService adminGameService;

    private Game game1;
    private Game game2;

    @BeforeEach
    void setUp() {
        game1 = GameFixture.FINISHED_GAME();
        setId(game1, 1L);

        game2 = GameFixture.WAITING_GAME();
        setId(game2, 2L);
    }

    @Nested
    @DisplayName("게임 목록 조회")
    class GetGameList {

        @Test
        void 필터_없이_게임_목록을_조회하면_전체_게임_페이지를_반환한다() {
            // given
            AdminGameListCommand command = new AdminGameListCommand(0, 10, null, SortDirection.DESC);
            Page<Game> gamePage = new PageImpl<>(List.of(game1, game2), PageRequest.of(0, 10), 2);
            given(gameRepository.findAllForAdmin(isNull(), any())).willReturn(gamePage);
            given(gameParticipantRepository.countByGameIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(
                            new GameParticipantCountProjection(1L, 3L),
                            new GameParticipantCountProjection(2L, 5L)
                    ));

            // when
            AdminGamePageResult result = adminGameService.getGameList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(2);
                softly.assertThat(result.content()).hasSize(2);
                softly.assertThat(result.content().get(0).participantCount()).isEqualTo(3);
                softly.assertThat(result.content().get(1).participantCount()).isEqualTo(5);
            });
        }

        @Test
        void 상태_필터로_게임_목록을_조회하면_해당_상태_게임만_반환한다() {
            // given
            AdminGameListCommand command = new AdminGameListCommand(0, 10, GameStatus.FINISHED, SortDirection.DESC);
            Page<Game> gamePage = new PageImpl<>(List.of(game1), PageRequest.of(0, 10), 1);
            given(gameRepository.findAllForAdmin(any(GameStatus.class), any())).willReturn(gamePage);
            given(gameParticipantRepository.countByGameIdIn(List.of(1L)))
                    .willReturn(List.of(new GameParticipantCountProjection(1L, 3L)));

            // when
            AdminGamePageResult result = adminGameService.getGameList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(1);
                softly.assertThat(result.content().get(0).status()).isEqualTo(GameStatus.FINISHED);
            });
        }

        @Test
        void 참여자가_없는_게임은_참여자_수가_0으로_반환된다() {
            // given
            AdminGameListCommand command = new AdminGameListCommand(0, 10, null, SortDirection.DESC);
            Page<Game> gamePage = new PageImpl<>(List.of(game1), PageRequest.of(0, 10), 1);
            given(gameRepository.findAllForAdmin(isNull(), any())).willReturn(gamePage);
            given(gameParticipantRepository.countByGameIdIn(List.of(1L))).willReturn(List.of());

            // when
            AdminGamePageResult result = adminGameService.getGameList(command);

            // then
            assertThat(result.content().get(0).participantCount()).isZero();
        }

        @Test
        void 결과가_없으면_빈_페이지를_반환한다() {
            // given
            AdminGameListCommand command = new AdminGameListCommand(0, 10, GameStatus.IN_PROGRESS, SortDirection.ASC);
            Page<Game> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(gameRepository.findAllForAdmin(any(GameStatus.class), any())).willReturn(emptyPage);
            given(gameParticipantRepository.countByGameIdIn(List.of())).willReturn(List.of());

            // when
            AdminGamePageResult result = adminGameService.getGameList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isZero();
                softly.assertThat(result.content()).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("게임 단건 조회")
    class GetGame {

        @Test
        void 게임_아이디로_게임을_조회하면_해당_게임_정보를_반환한다() {
            // given
            given(gameRepository.getByGameId(1L)).willReturn(game1);

            // when
            AdminGameResult result = adminGameService.getGame(1L);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.id()).isEqualTo(1L);
                softly.assertThat(result.status()).isEqualTo(GameStatus.FINISHED);
            });
        }
    }

    @Nested
    @DisplayName("게임별 참여자 배치 조회")
    class GetParticipantsByGame {

        @Test
        void 참여자가_있는_게임의_참여자_목록을_반환한다() {
            // given
            User user = UserFixture.USER("player1");
            setId(user, 10L);

            GameParticipant participant = GameParticipantFixture.HOST_PARTICIPANT(game1, user);
            setId(participant, 100L);

            AdminGameResult adminGame1 = AdminGameResult.from(game1);
            AdminGameResult adminGame2 = AdminGameResult.from(game2);
            List<AdminGameResult> games = List.of(adminGame1, adminGame2);

            given(gameParticipantRepository.findByGameIdInWithUser(List.of(1L, 2L)))
                    .willReturn(List.of(participant));

            // when
            Map<AdminGameResult, List<AdminParticipantResult>> result =
                    adminGameService.getParticipantsByGame(games);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(2);
                softly.assertThat(result.get(adminGame1)).hasSize(1);
                softly.assertThat(result.get(adminGame2)).isEmpty();
            });
        }

        @Test
        void 참여자가_없는_게임은_빈_리스트를_반환한다() {
            // given
            AdminGameResult adminGame = AdminGameResult.from(game1);
            given(gameParticipantRepository.findByGameIdInWithUser(anyList())).willReturn(List.of());

            // when
            Map<AdminGameResult, List<AdminParticipantResult>> result =
                    adminGameService.getParticipantsByGame(List.of(adminGame));

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.get(adminGame)).isEmpty();
            });
        }

        @Test
        void 한_게임에_여러_참여자가_있는_경우_모두_반환한다() {
            // given
            User user1 = UserFixture.USER("player1");
            setId(user1, 10L);
            User user2 = UserFixture.KAKAO_USER();
            setId(user2, 11L);

            GameParticipant participant1 = GameParticipantFixture.HOST_PARTICIPANT(game1, user1);
            setId(participant1, 100L);
            GameParticipant participant2 = GameParticipantFixture.GUEST_PARTICIPANT(game1, user2);
            setId(participant2, 101L);

            AdminGameResult adminGame = AdminGameResult.from(game1);
            given(gameParticipantRepository.findByGameIdInWithUser(List.of(1L)))
                    .willReturn(List.of(participant1, participant2));

            // when
            Map<AdminGameResult, List<AdminParticipantResult>> result =
                    adminGameService.getParticipantsByGame(List.of(adminGame));

            // then
            assertThat(result.get(adminGame)).hasSize(2);
        }
    }

    @Nested
    @DisplayName("게임별 결과 배치 조회")
    class GetResultsByGame {

        @Test
        void 결과가_있는_게임만_결과_맵에_포함된다() {
            // given
            GameResult gameResult = GameResultFixture.POLICE_WIN_RESULT(1L);

            AdminGameResult adminGame1 = AdminGameResult.from(game1);
            AdminGameResult adminGame2 = AdminGameResult.from(game2);
            List<AdminGameResult> games = List.of(adminGame1, adminGame2);

            given(gameResultRepository.findByGameIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(gameResult));

            // when
            Map<AdminGameResult, AdminGameDetailResult> result =
                    adminGameService.getResultsByGame(games);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result).containsKey(adminGame1);
                softly.assertThat(result).doesNotContainKey(adminGame2);
            });
        }

        @Test
        void 결과가_없으면_빈_맵을_반환한다() {
            // given
            AdminGameResult adminGame = AdminGameResult.from(game1);
            given(gameResultRepository.findByGameIdIn(anyList())).willReturn(List.of());

            // when
            Map<AdminGameResult, AdminGameDetailResult> result =
                    adminGameService.getResultsByGame(List.of(adminGame));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("게임별 구역 배치 조회")
    class GetAreasByGame {

        @Test
        void 구역이_있는_게임만_구역_맵에_포함된다() {
            // given
            GameArea gameArea = GameAreaFixture.CIRCLE_GAME_AREA(game1);

            AdminGameResult adminGame1 = AdminGameResult.from(game1);
            AdminGameResult adminGame2 = AdminGameResult.from(game2);
            List<AdminGameResult> games = List.of(adminGame1, adminGame2);

            given(gameAreaRepository.findByGameIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(gameArea));

            // when
            Map<AdminGameResult, AdminGameAreaResult> result =
                    adminGameService.getAreasByGame(games);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result).containsKey(adminGame1);
                softly.assertThat(result).doesNotContainKey(adminGame2);
            });
        }

        @Test
        void 구역이_없으면_빈_맵을_반환한다() {
            // given
            AdminGameResult adminGame = AdminGameResult.from(game1);
            given(gameAreaRepository.findByGameIdIn(anyList())).willReturn(List.of());

            // when
            Map<AdminGameResult, AdminGameAreaResult> result =
                    adminGameService.getAreasByGame(List.of(adminGame));

            // then
            assertThat(result).isEmpty();
        }
    }
}
