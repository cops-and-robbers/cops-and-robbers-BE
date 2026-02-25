package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class GameSchedulerServiceTest extends ServiceUnitTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW.atZone(KST).toInstant(), KST);

    @InjectMocks
    private GameSchedulerService gameSchedulerService;

    @Spy
    private Clock clock = FIXED_CLOCK;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private SystemPublisher systemPublisher;

    @Mock
    private SystemEventFactory systemEventFactory;

    @Mock
    private RobberLocationService robberLocationService;

    private static final Long TEST_GAME_ID = 1L;

    private static final int POLICE_WAIT_MINUTES = 3;
    private static final int ROUND_DURATION_MINUTES = 30;
    private static final int LOCATION_REVEAL_INTERVAL_MINUTES = 5;

    @Nested
    @DisplayName("정상 접속 시 게임 이벤트 예약")
    class ScheduleAllEvents {

        @Test
        void 경찰_이동_시작_이벤트가_대기시간_후에_예약된다() {
            // given
            LocalDateTime startedAt = FIXED_NOW;
            Game game = createGameStartedAt(startedAt);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            mockSchedule();

            // when
            gameSchedulerService.scheduleAllEvents(TEST_GAME_ID);

            // then
            List<Instant> scheduledTimes = captureScheduledInstants();
            Instant expected = toInstant(clock, startedAt.plusMinutes(POLICE_WAIT_MINUTES));
            assertThat(scheduledTimes).contains(expected);
        }

        @Test
        void 도둑_위치_공개_이벤트가_주기적으로_예약된다() {
            // given
            LocalDateTime startedAt = FIXED_NOW;
            Game game = createGameStartedAt(startedAt);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            mockSchedule();

            // when
            gameSchedulerService.scheduleAllEvents(TEST_GAME_ID);

            // then
            List<Instant> scheduledTimes = captureScheduledInstants();

            int revealCount = (ROUND_DURATION_MINUTES - POLICE_WAIT_MINUTES) / LOCATION_REVEAL_INTERVAL_MINUTES;
            for (int i = 1; i <= revealCount; i++) {
                int revealMinutes = POLICE_WAIT_MINUTES + LOCATION_REVEAL_INTERVAL_MINUTES * i;
                Instant expectedReveal = toInstant(clock, startedAt.plusMinutes(revealMinutes));
                assertThat(scheduledTimes).contains(expectedReveal);
            }
        }

        @Test
        void 이미_지난_시간의_이벤트는_예약되지_않는다() {
            // given
            LocalDateTime finishedTime = FIXED_NOW
                            .minusMinutes(ROUND_DURATION_MINUTES)
                            .minusMinutes(10);
            Game game = createGameStartedAt(finishedTime); // 게임 종료 시각 + 10분 더 지난 상태 → 모든 이벤트 시점이 과거

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);

            // when
            gameSchedulerService.scheduleAllEvents(TEST_GAME_ID);

            // then
            then(taskScheduler).should(never()).schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        void 게임_종료_시간_이후의_이벤트는_예약되지_않는다() {
            // given
            LocalDateTime startedAt = FIXED_NOW.minusMinutes(25); // 25분 전에 시작 → 총 30분 게임이므로 현재 5분만 남은 상태
            Game game = createGameStartedAt(startedAt);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            mockSchedule();

            // when
            gameSchedulerService.scheduleAllEvents(TEST_GAME_ID);

            // then
            List<Instant> scheduledTimes = captureScheduledInstants();
            Instant gameOverTime = toInstant(clock, startedAt.plusMinutes(ROUND_DURATION_MINUTES));
            assertThat(scheduledTimes).allMatch(instant -> instant.isBefore(gameOverTime));
        }
    }

    @Nested
    @DisplayName("재접속 시 게임 이벤트 예약")
    class RecoverSchedules {

        @Test
        void 진행_중인_게임이_있으면_스케줄이_복구된다() {
            // given
            LocalDateTime startedAt = FIXED_NOW;
            Game game = createGameStartedAt(startedAt);

            given(gameRepository.findByStatus(GameStatus.IN_PROGRESS)).willReturn(List.of(game));
            mockSchedule();

            // when
            gameSchedulerService.recoverSchedules();

            // then
            List<Instant> scheduledTimes = captureScheduledInstants();
            Instant expected = toInstant(clock, startedAt.plusMinutes(POLICE_WAIT_MINUTES));
            assertThat(scheduledTimes).contains(expected);
        }

        @Test
        void 진행_중인_게임이_없으면_스케줄이_복구되지_않는다() {
            // given
            given(gameRepository.findByStatus(GameStatus.IN_PROGRESS)).willReturn(Collections.emptyList());

            // when
            gameSchedulerService.recoverSchedules();

            // then
            then(taskScheduler).should(never()).schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        void 이미_지난_게임은_재예약되지_않는다() {
            // given
            LocalDateTime finishedTime = FIXED_NOW
                            .minusMinutes(ROUND_DURATION_MINUTES)
                            .minusMinutes(10);
            Game game = createGameStartedAt(finishedTime);

            given(gameRepository.findByStatus(GameStatus.IN_PROGRESS)).willReturn(List.of(game));

            // when
            gameSchedulerService.recoverSchedules();

            // then
            then(taskScheduler).should(never()).schedule(any(Runnable.class), any(Instant.class));
        }
    }

    private Game createGameStartedAt(LocalDateTime startedAt) {
        Game game = IN_PROGRESS_GAME(startedAt);
        setId(game, TEST_GAME_ID);
        return game;
    }

    private void mockSchedule() {
        given(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .willReturn(mock(ScheduledFuture.class));
    }

    // 스케줄러 호출 시 전달된 Instant(예약 실행 시각)들을 모두 수집하는 헬퍼 메서드
    private List<Instant> captureScheduledInstants() {
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        then(taskScheduler).should(atLeastOnce()).schedule(any(Runnable.class), captor.capture());
        return captor.getAllValues();
    }

    public static Instant toInstant(Clock clock, LocalDateTime localDateTime) {
        return localDateTime.atZone(clock.getZone()).toInstant();
    }
}
