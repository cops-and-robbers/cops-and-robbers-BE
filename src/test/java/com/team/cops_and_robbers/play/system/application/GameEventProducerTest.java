package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.play.system.infrastructure.GameScheduleQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class GameEventProducerTest extends ServiceUnitTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW.atZone(KST).toInstant(), KST);

    @InjectMocks
    private GameEventProducer gameEventProducer;

    @Spy
    private Clock clock = FIXED_CLOCK;

    @Mock
    private GameScheduleQueue gameScheduleQueue;

    @Mock
    private GameTerminationService gameTerminationService;

    private static final Long TEST_GAME_ID = 1L;

    @Nested
    @DisplayName("게임 시작 시 이벤트 등록")
    class ScheduleAllEvents {

        @Test
        void 정상_시작_시_세_가지_이벤트가_모두_등록된다() {
            // given
            Game game = IN_PROGRESS_GAME(FIXED_NOW);
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);

            // when
            gameEventProducer.scheduleAllEvents(TEST_GAME_ID);

            // then
            then(gameScheduleQueue).should().enqueuePoliceMoveStart(game, FIXED_NOW);
            then(gameScheduleQueue).should().enqueueFirstReveal(game, FIXED_NOW);
            then(gameScheduleQueue).should().enqueueGameOver(game, FIXED_NOW);
        }

        @Test
        void 종료_시각이_이미_지난_경우_즉시_종료되고_큐에_등록되지_않는다() {
            // given: 40분 전 시작, 30분 게임 → 이미 10분 전 종료
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(40));
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);

            // when
            gameEventProducer.scheduleAllEvents(TEST_GAME_ID);

            // then
            then(gameScheduleQueue).should(never()).enqueueGameOver(any(Game.class), any(LocalDateTime.class));
            then(gameTerminationService).should().endGameByTimeOver(TEST_GAME_ID);
        }
    }
}
