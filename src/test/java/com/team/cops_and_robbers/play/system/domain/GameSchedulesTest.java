package com.team.cops_and_robbers.play.system.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class GameSchedulesTest {

    private GameSchedules gameSchedules;
    private static final Long GAME_ID = 1L;

    @BeforeEach
    void setUp() {
        gameSchedules = new GameSchedules();
    }

    @Nested
    @DisplayName("addTask - GAME_OVER 태스크 설정")
    class AddGameOverTask {

        @Test
        void addTask_호출_시_cancelAll에서_포함되어_취소된다() {
            // given
            ScheduledFuture<?> otherTask = mock(ScheduledFuture.class);
            gameSchedules.register(GAME_ID);
            gameSchedules.addTask(GAME_ID, TaskType.POLICE_MOVE_START, otherTask);

            ScheduledFuture<?> gameOverTask = mock(ScheduledFuture.class);

            // when
            gameSchedules.addTask(GAME_ID, TaskType.GAME_OVER, gameOverTask);

            // then: cancelAll이 게임오버 태스크 포함 총 2개를 취소해야 한다
            int cancelledCount = gameSchedules.cancelAll(GAME_ID);
            assertThat(cancelledCount).isEqualTo(2);
        }

        @Test
        void 등록되지_않은_gameId에_addTask_호출_시_예외가_발생한다() {
            ScheduledFuture<?> future = mock(ScheduledFuture.class);

            assertThatThrownBy(() -> gameSchedules.addTask(GAME_ID, TaskType.GAME_OVER, future))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(String.valueOf(GAME_ID));
        }
    }

    @Nested
    @DisplayName("cancelByType - GAME_OVER 태스크만 선택적 취소")
    class CancelByType {

        @Test
        void cancelByType_호출_시_해당_타입만_취소되고_나머지는_유지된다() {
            // given
            ScheduledFuture<?> otherTask = mock(ScheduledFuture.class);
            gameSchedules.register(GAME_ID);
            gameSchedules.addTask(GAME_ID, TaskType.POLICE_MOVE_START, otherTask);

            ScheduledFuture<?> gameOverTask = mock(ScheduledFuture.class);
            gameSchedules.addTask(GAME_ID, TaskType.GAME_OVER, gameOverTask);

            // when
            boolean cancelled = gameSchedules.cancelByType(GAME_ID, TaskType.GAME_OVER);

            // then: gameOverTask는 취소됨
            assertThat(cancelled).isTrue();
            then(gameOverTask).should().cancel(false);

            // otherTask는 아직 남아 있어 cancelAll에서 처리
            int cancelledCount = gameSchedules.cancelAll(GAME_ID);
            assertThat(cancelledCount).isEqualTo(1);
        }

        @Test
        void 해당_타입_태스크가_없는_경우_cancelByType은_false를_반환한다() {
            // given: game over task 없이 등록
            gameSchedules.register(GAME_ID);

            // when
            boolean cancelled = gameSchedules.cancelByType(GAME_ID, TaskType.GAME_OVER);

            // then
            assertThat(cancelled).isFalse();
        }

        @Test
        void 취소_후_다시_cancelByType_호출_시_false를_반환한다() {
            // given
            gameSchedules.register(GAME_ID);
            ScheduledFuture<?> gameOverTask = mock(ScheduledFuture.class);
            gameSchedules.addTask(GAME_ID, TaskType.GAME_OVER, gameOverTask);

            gameSchedules.cancelByType(GAME_ID, TaskType.GAME_OVER); // first cancel

            // when
            boolean secondCancel = gameSchedules.cancelByType(GAME_ID, TaskType.GAME_OVER);

            // then
            assertThat(secondCancel).isFalse();
            then(gameOverTask).should().cancel(false); // called exactly once
        }
    }

    @Nested
    @DisplayName("cancelAll - 전체 태스크 취소 및 엔트리 제거")
    class CancelAll {

        @Test
        void cancelAll_호출_시_모든_태스크가_취소되고_엔트리가_제거된다() {
            // given
            ScheduledFuture<?> task1 = mock(ScheduledFuture.class);
            ScheduledFuture<?> task2 = mock(ScheduledFuture.class);
            ScheduledFuture<?> gameOverTask = mock(ScheduledFuture.class);

            gameSchedules.register(GAME_ID);
            gameSchedules.addTask(GAME_ID, TaskType.POLICE_MOVE_START, task1);
            gameSchedules.addTask(GAME_ID, TaskType.LOCATION_REVEAL, task2);
            gameSchedules.addTask(GAME_ID, TaskType.GAME_OVER, gameOverTask);

            // when
            int count = gameSchedules.cancelAll(GAME_ID);

            // then: task1, task2, gameOverTask 총 3개
            assertThat(count).isEqualTo(3);
            then(task1).should().cancel(false);
            then(task2).should().cancel(false);
            then(gameOverTask).should().cancel(false);
        }

        @Test
        void cancelAll_후_다시_cancelAll_호출_시_0을_반환한다() {
            // given
            gameSchedules.register(GAME_ID);
            gameSchedules.addTask(GAME_ID, TaskType.POLICE_MOVE_START, mock(ScheduledFuture.class));
            gameSchedules.cancelAll(GAME_ID);

            // when
            int count = gameSchedules.cancelAll(GAME_ID);

            // then: 엔트리가 이미 제거됐으므로 0
            assertThat(count).isEqualTo(0);
        }

        @Test
        void 등록되지_않은_gameId에_cancelAll_호출_시_0을_반환한다() {
            int count = gameSchedules.cancelAll(GAME_ID);
            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("addTask - 추가 태스크 등록")
    class AddTask {

        @Test
        void addTask_호출_시_태스크가_추가된다() {
            // given
            gameSchedules.register(GAME_ID);
            ScheduledFuture<?> extraTask = mock(ScheduledFuture.class);

            // when
            gameSchedules.addTask(GAME_ID, TaskType.ADDITIONAL_TIME_EVALUATION, extraTask);

            // then
            int count = gameSchedules.cancelAll(GAME_ID);
            assertThat(count).isEqualTo(1);
            then(extraTask).should().cancel(false);
        }

        @Test
        void 등록되지_않은_gameId에_addTask_호출_시_예외가_발생한다() {
            ScheduledFuture<?> future = mock(ScheduledFuture.class);

            assertThatThrownBy(() -> gameSchedules.addTask(GAME_ID, TaskType.ADDITIONAL_TIME_EVALUATION, future))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(String.valueOf(GAME_ID));
        }
    }
}
