package com.team.cops_and_robbers.history.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.history.application.dto.command.GameResultCommand;
import com.team.cops_and_robbers.history.application.dto.result.GameResultResult;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.exception.GameResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.CIRCLE_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.POLYGON_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameResultFixture.POLICE_WIN_RESULT;
import static com.team.cops_and_robbers.common.fixture.GameResultFixture.POLICE_WIN_RESULT_POLYGON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class GameResultServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameResultService gameResultService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_GAME_ID = 1L;
    private static final Long TEST_GAME_RESULT_ID = 1L;

    @Nested
    @DisplayName("게임 결과 기록")
    class RecordGameResult {

        private static final Long TEST_GAME_ID = 1L;

        @Test
        void CIRCLE_영역_게임이_종료되면_circle_필드가_채워진_결과가_저장된다() {
            // given
            Game game = IN_PROGRESS_GAME();
            setId(game, TEST_GAME_ID);
            GameArea circleArea = CIRCLE_GAME_AREA(game);

            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(circleArea);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(2);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.ROBBER)).willReturn(3);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.JAILED)).willReturn(3);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            GameResult result = gameResultService.recordGameResult(game, Team.POLICE, GameEndReason.ALL_ARRESTED);

            // then
            assertThat(result.getAreaType()).isEqualTo(AreaType.CIRCLE);
            assertThat(result.getPlaygroundCenter()).isNotNull();
            assertThat(result.getPlaygroundRadiusInMeters()).isNotNull();
            assertThat(result.getPlaygroundPolygon()).isNull();
            then(gameResultRepository).should().save(any(GameResult.class));
        }

        @Test
        void POLYGON_영역_게임이_종료되면_polygon_필드가_채워진_결과가_저장된다() {
            // given
            Game game = IN_PROGRESS_GAME();
            setId(game, TEST_GAME_ID);
            GameArea polygonArea = POLYGON_GAME_AREA(game);

            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(polygonArea);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(2);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.ROBBER)).willReturn(3);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.JAILED)).willReturn(3);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            GameResult result = gameResultService.recordGameResult(game, Team.POLICE, GameEndReason.ALL_ARRESTED);

            // then
            assertThat(result.getAreaType()).isEqualTo(AreaType.POLYGON);
            assertThat(result.getPlaygroundPolygon()).isNotNull();
            assertThat(result.getPlaygroundCenter()).isNull();
            assertThat(result.getPlaygroundRadiusInMeters()).isNull();
            then(gameResultRepository).should().save(any(GameResult.class));
        }
    }

    @Nested
    @DisplayName("게임 결과 조회")
    class GetGameResult {

        @Test
        void CIRCLE_영역_게임_결과_조회에_성공한다() {
            // given
            GameResult gameResult = POLICE_WIN_RESULT(TEST_GAME_ID);
            setId(gameResult, TEST_GAME_RESULT_ID);

            given(gameResultRepository.findById(TEST_GAME_RESULT_ID)).willReturn(Optional.of(gameResult));

            // when
            GameResultResult result = gameResultService.getGameResult(GameResultCommand.of(TEST_USER_ID, TEST_GAME_RESULT_ID));

            // then
            assertThat(result.winnerTeam()).isEqualTo(gameResult.getWinnerTeam());
            assertThat(result.durationSeconds()).isEqualTo(gameResult.getDurationSeconds());
            assertThat(result.totalArrestCount()).isEqualTo(gameResult.getTotalArrestCount());
            assertThat(result.remainingRobberCount())
                    .isEqualTo(gameResult.getTotalRobberCount() - gameResult.getArrestedRobberCount());
        }

        @Test
        void POLYGON_영역_게임_결과_조회에_성공한다() {
            // given
            GameResult gameResult = POLICE_WIN_RESULT_POLYGON(TEST_GAME_ID);
            setId(gameResult, TEST_GAME_RESULT_ID);

            given(gameResultRepository.findById(TEST_GAME_RESULT_ID)).willReturn(Optional.of(gameResult));

            // when
            GameResultResult result = gameResultService.getGameResult(GameResultCommand.of(TEST_USER_ID, TEST_GAME_RESULT_ID));

            // then
            assertThat(result.winnerTeam()).isEqualTo(gameResult.getWinnerTeam());
            assertThat(result.durationSeconds()).isEqualTo(gameResult.getDurationSeconds());
            assertThat(result.totalArrestCount()).isEqualTo(gameResult.getTotalArrestCount());
            assertThat(result.remainingRobberCount())
                    .isEqualTo(gameResult.getTotalRobberCount() - gameResult.getArrestedRobberCount());
        }

        @Test
        void 게임_결과가_존재하지_않으면_예외가_발생한다() {
            // given
            given(gameResultRepository.findById(TEST_GAME_RESULT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> gameResultService.getGameResult(GameResultCommand.of(TEST_USER_ID, TEST_GAME_RESULT_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameResultException.GAME_RESULT_NOT_FOUND.getDetail());
        }

        @Test
        void 해당_게임의_참가자가_아니면_예외가_발생한다() {
            // given
            GameResult gameResult = POLICE_WIN_RESULT(TEST_GAME_ID);
            setId(gameResult, TEST_GAME_RESULT_ID);

            given(gameResultRepository.findById(TEST_GAME_RESULT_ID)).willReturn(Optional.of(gameResult));
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID))
                    .willThrow(new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> gameResultService.getGameResult(GameResultCommand.of(TEST_USER_ID, TEST_GAME_RESULT_ID)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.PARTICIPANT_NOT_FOUND.getDetail());
        }
    }
}
