package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.history.application.GameResultService;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import static com.team.cops_and_robbers.common.fixture.GameFixture.FINISHED_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class GameTerminationServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameTerminationService gameTerminationService;

    @Mock
    private GameResultService gameResultService;

    @Mock
    private SystemEventFactory systemEventFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final Long TEST_GAME_ID = 1L;

    @Nested
    @DisplayName("전원 체포로 게임 종료")
    class EndGameByAllArrested {

        @Test
        void 게임_종료_시_게임_종료_이벤트가_발행된다() {
            // given
            Game game = IN_PROGRESS_GAME();
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameResultService.recordGameResult(any(), any(), any())).willReturn(mock(GameResult.class));
            given(systemEventFactory.createGameEndEvent(any(), any(), any(), any())).willReturn(mock(SystemEvent.class));

            // when
            gameTerminationService.endGameByAllArrested(TEST_GAME_ID);

            // then
            then(eventPublisher).should().publishEvent(any(SystemEvent.class));
        }

        @Test
        void 이미_종료된_게임은_이벤트가_발행되지_않는다() {
            // given
            Game game = FINISHED_GAME();
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);

            // when
            gameTerminationService.endGameByAllArrested(TEST_GAME_ID);

            // then
            then(eventPublisher).should(never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("타임오버로 게임 종료")
    class EndGameByTimeOver {

        @Test
        void 게임_종료_시_게임_종료_이벤트가_발행된다() {
            // given
            Game game = IN_PROGRESS_GAME();
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameResultService.recordGameResult(any(), any(), any())).willReturn(mock(GameResult.class));
            given(systemEventFactory.createGameEndEvent(any(), any(), any(), any())).willReturn(mock(SystemEvent.class));

            // when
            gameTerminationService.endGameByTimeOver(TEST_GAME_ID);

            // then
            then(eventPublisher).should().publishEvent(any(SystemEvent.class));
        }

        @Test
        void 이미_종료된_게임은_이벤트가_발행되지_않는다() {
            // given
            Game game = FINISHED_GAME();
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);

            // when
            gameTerminationService.endGameByTimeOver(TEST_GAME_ID);

            // then
            then(eventPublisher).should(never()).publishEvent(any());
        }
    }
}
