package com.team.cops_and_robbers.history.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.history.application.dto.result.GameResultResult;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.exception.GameResultException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.GameResultFixture.POLICE_WIN_RESULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class GameResultServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameResultService gameResultService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_GAME_ID = 1L;
    private static final Long TEST_GAME_RESULT_ID = 1L;

    @Nested
    @DisplayName("게임 결과 조회")
    class GetGameResult {

        @Test
        void 게임_결과_조회에_성공한다() {
            // given
            GameResult gameResult = POLICE_WIN_RESULT(TEST_GAME_ID);
            setId(gameResult, TEST_GAME_RESULT_ID);

            given(gameResultRepository.findById(TEST_GAME_RESULT_ID)).willReturn(Optional.of(gameResult));

            // when
            GameResultResult result = gameResultService.getGameResult(TEST_GAME_RESULT_ID, TEST_USER_ID);

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
            assertThatThrownBy(() -> gameResultService.getGameResult(TEST_GAME_RESULT_ID, TEST_USER_ID))
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
            assertThatThrownBy(() -> gameResultService.getGameResult(TEST_GAME_RESULT_ID, TEST_USER_ID))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.PARTICIPANT_NOT_FOUND.getDetail());
        }
    }
}
