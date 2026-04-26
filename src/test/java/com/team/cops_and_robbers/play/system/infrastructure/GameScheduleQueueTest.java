package com.team.cops_and_robbers.play.system.infrastructure;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.play.system.domain.GameScheduleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GameScheduleQueueTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW.atZone(KST).toInstant(), KST);

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBlockingQueue<GameScheduleEvent> blockingQueue;

    @Mock
    private RDelayedQueue<GameScheduleEvent> delayedQueue;

    private GameScheduleQueue gameScheduleQueue;

    @BeforeEach
    void setUp() {
        doReturn(blockingQueue).when(redissonClient).getBlockingQueue(anyString(), any());
        doReturn(delayedQueue).when(redissonClient).getDelayedQueue(any());
        gameScheduleQueue = new GameScheduleQueue(FIXED_CLOCK, redissonClient);
    }

    @Nested
    @DisplayName("경찰 출동 이벤트 등록")
    class EnqueuePoliceMoveStart {

        @Test
        void 경찰_대기_시각이_미래면_이벤트가_등록된다() {
            // given: 방금 시작된 게임 (policeWaitMinutes=3 → 3분 후 출동)
            Game game = IN_PROGRESS_GAME(FIXED_NOW);

            // when
            gameScheduleQueue.enqueuePoliceMoveStart(game, FIXED_NOW);

            // then
            then(delayedQueue).should().offer(any(), anyLong(), any(TimeUnit.class));
        }

        @Test
        void 경찰_대기_시각이_이미_지났으면_등록되지_않는다() {
            // given: 10분 전 시작, policeWaitMinutes=3 → 7분 전에 이미 출동
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(10));

            // when
            gameScheduleQueue.enqueuePoliceMoveStart(game, FIXED_NOW);

            // then
            then(delayedQueue).should(never()).offer(any(), anyLong(), any(TimeUnit.class));
        }
    }

    @Nested
    @DisplayName("도둑 위치 첫 공개 이벤트 등록")
    class EnqueueFirstReveal {

        @Test
        void 첫_공개_시각이_미래면_이벤트가_등록된다() {
            // given: 방금 시작 (policeWait=3 + interval=5 → 8분 후 첫 공개)
            Game game = IN_PROGRESS_GAME(FIXED_NOW);

            // when
            gameScheduleQueue.enqueueFirstReveal(game, FIXED_NOW);

            // then
            then(delayedQueue).should().offer(any(), anyLong(), any(TimeUnit.class));
        }

        @Test
        void 첫_공개_시각이_이미_지났으면_등록되지_않는다() {
            // given: 20분 전 시작 (8분 후 첫 공개 → 12분 전에 이미 공개됨)
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(20));

            // when
            gameScheduleQueue.enqueueFirstReveal(game, FIXED_NOW);

            // then
            then(delayedQueue).should(never()).offer(any(), anyLong(), any(TimeUnit.class));
        }
    }

    @Nested
    @DisplayName("도둑 위치 재등록")
    class EnqueueNextReveal {

        @Test
        void 다음_공개_시각이_게임_종료_전이면_재등록된다() {
            // given: 방금 시작 (interval=5 → 5분 후 재등록, gameOver=30분 후)
            Game game = IN_PROGRESS_GAME(FIXED_NOW);

            // when
            gameScheduleQueue.enqueueNextReveal(game);

            // then
            then(delayedQueue).should().offer(any(), anyLong(), any(TimeUnit.class));
        }

        @Test
        void 다음_공개_시각이_게임_종료_후면_재등록되지_않는다() {
            // given: 26분 전 시작 (gameOver=4분 후, interval=5 → 다음 공개=5분 후 → 종료 이후)
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(26));

            // when
            gameScheduleQueue.enqueueNextReveal(game);

            // then
            then(delayedQueue).should(never()).offer(any(), anyLong(), any(TimeUnit.class));
        }
    }

    @Nested
    @DisplayName("게임 종료 이벤트 등록")
    class EnqueueGameOver {

        @Test
        void 종료_시각이_미래면_이벤트가_등록된다() {
            // given: 방금 시작 (roundDuration=30 → 30분 후 종료)
            Game game = IN_PROGRESS_GAME(FIXED_NOW);

            // when
            gameScheduleQueue.enqueueGameOver(game, FIXED_NOW);

            // then
            then(delayedQueue).should().offer(any(), anyLong(), any(TimeUnit.class));
        }

        @Test
        void 종료_시각이_이미_지났으면_등록되지_않는다() {
            // given: 40분 전 시작 (30분 게임 → 10분 전 이미 종료)
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(40));

            // when
            gameScheduleQueue.enqueueGameOver(game, FIXED_NOW);

            // then
            then(delayedQueue).should(never()).offer(any(), anyLong(), any(TimeUnit.class));
        }
    }
}