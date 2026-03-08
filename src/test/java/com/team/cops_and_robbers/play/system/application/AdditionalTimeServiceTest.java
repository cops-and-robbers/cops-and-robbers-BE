package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class AdditionalTimeServiceTest extends ServiceUnitTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW.atZone(KST).toInstant(), KST);

    @InjectMocks
    private AdditionalTimeService additionalTimeService;

    @Mock
    private GameTerminationService gameTerminationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SystemEventFactory systemEventFactory;

    @Spy
    private Clock clock = FIXED_CLOCK;

    private static final Long TEST_GAME_ID = 1L;

    @Nested
    @DisplayName("startAdditionalTime - 추가 시간 시작")
    class StartAdditionalTime {

        @Test
        void 게임이_IN_PROGRESS가_아니면_추가시간을_시작하지_않는다() {
            // given
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(10));
            game.resetForNextRound();
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);

            // when
            additionalTimeService.startAdditionalTime(TEST_GAME_ID, GameEndReason.ALL_ARRESTED);

            // then
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        void 이미_추가시간_상태이면_중복으로_시작하지_않는다() {
            // given
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(10));
            game.startAdditionalTime(FIXED_NOW);
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);

            // when
            additionalTimeService.startAdditionalTime(TEST_GAME_ID, GameEndReason.ALL_ARRESTED);

            // then
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        void 추가시간이_시작되면_게임_상태를_변경하고_이벤트를_발행한다() {
            // given
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(10));
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(systemEventFactory.createAdditionalTimeStartedEvent(any(), any(), any(Integer.class)))
                    .willReturn(mockAdditionalTimeStartedEvent(GameEndReason.ALL_ARRESTED));

            // when
            additionalTimeService.startAdditionalTime(TEST_GAME_ID, GameEndReason.ALL_ARRESTED);

            // then
            assertThat(game.isInAdditionalTime()).isTrue();
            then(eventPublisher).should().publishEvent(any(SystemEvent.class));
        }
    }

    @Nested
    @DisplayName("evaluateAdditionalTime - ALL_ARRESTED 트리거")
    class EvaluateAllArrested {

        @Test
        void 전원_체포_상태이면_경찰_승리로_종료한다() {
            // given
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(10));
            game.startAdditionalTime(FIXED_NOW);
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.ALIVE)).willReturn(0);

            // when
            additionalTimeService.evaluateAdditionalTime(TEST_GAME_ID, GameEndReason.ALL_ARRESTED);

            // then
            then(gameTerminationService).should().endGame(game, Team.POLICE, GameEndReason.ALL_ARRESTED);
        }

        @Test
        void 탈옥자가_발생했으면_GAME_RESUMED_이벤트를_발행하고_게임을_속행한다() {
            // given
            LocalDateTime startedAt = FIXED_NOW.minusMinutes(5); // 5분 경과, 총 30분 → 25분 남음
            Game game = IN_PROGRESS_GAME(startedAt);
            game.startAdditionalTime(FIXED_NOW);
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.ALIVE)).willReturn(1);
            given(systemEventFactory.createGameResumedEvent(eq(TEST_GAME_ID), any(Long.class)))
                    .willReturn(mockGameResumedEvent());

            // when
            additionalTimeService.evaluateAdditionalTime(TEST_GAME_ID, GameEndReason.ALL_ARRESTED);

            // then
            then(gameTerminationService).should(never()).endGame(any(), any(), any());
            assertThat(game.isInAdditionalTime()).isFalse();

            // GAME_RESUMED 이벤트가 발행되어야 한다 (스케줄러 재등록은 SystemEventHandler가 처리)
            ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(SystemEventType.GAME_RESUMED);
        }

        @Test
        void 탈옥_후_원래_게임시간이_이미_지났으면_TIME_OVER_추가시간을_시작한다() {
            // given
            LocalDateTime startedAt = FIXED_NOW.minusMinutes(35); // 35분 경과, 총 30분 → 원래 종료 시간 경과
            Game game = IN_PROGRESS_GAME(startedAt);
            game.startAdditionalTime(FIXED_NOW);
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.ALIVE)).willReturn(1);
            given(systemEventFactory.createAdditionalTimeStartedEvent(any(), any(), any(Integer.class)))
                    .willReturn(mockAdditionalTimeStartedEvent(GameEndReason.TIME_OVER));

            // when
            additionalTimeService.evaluateAdditionalTime(TEST_GAME_ID, GameEndReason.ALL_ARRESTED);

            // then: endAdditionalTime → startAdditionalTime(TIME_OVER) 진입
            assertThat(game.isInAdditionalTime()).isTrue();
            ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());
            assertThat(captor.getValue().type()).isEqualTo(SystemEventType.ADDITIONAL_TIME_STARTED);
        }
    }

    @Nested
    @DisplayName("evaluateAdditionalTime - TIME_OVER 트리거")
    class EvaluateTimeOver {

        @Test
        void 생존_도둑이_있으면_도둑_승리로_종료한다() {
            // given
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(30));
            game.startAdditionalTime(FIXED_NOW);
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.ALIVE)).willReturn(2);

            // when
            additionalTimeService.evaluateAdditionalTime(TEST_GAME_ID, GameEndReason.TIME_OVER);

            // then
            then(gameTerminationService).should().endGame(game, Team.ROBBER, GameEndReason.TIME_OVER);
        }

        @Test
        void 전원_체포_상태이면_경찰_승리로_종료한다() {
            // given
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(30));
            game.startAdditionalTime(FIXED_NOW);
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.ALIVE)).willReturn(0);

            // when
            additionalTimeService.evaluateAdditionalTime(TEST_GAME_ID, GameEndReason.TIME_OVER);

            // then
            then(gameTerminationService).should().endGame(game, Team.POLICE, GameEndReason.ALL_ARRESTED);
        }

        @Test
        void 게임이_IN_PROGRESS가_아니면_평가를_건너뛴다() {
            // given
            Game game = IN_PROGRESS_GAME(FIXED_NOW.minusMinutes(30));
            game.resetForNextRound();
            setId(game, TEST_GAME_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);

            // when
            additionalTimeService.evaluateAdditionalTime(TEST_GAME_ID, GameEndReason.TIME_OVER);

            // then
            then(gameTerminationService).should(never()).endGame(any(), any(), any());
        }
    }

    private SystemEvent mockAdditionalTimeStartedEvent(GameEndReason trigger) {
        return SystemEvent.of(TEST_GAME_ID, SystemEventType.ADDITIONAL_TIME_STARTED,
                SystemEventData.AdditionalTimeStartedData.of(trigger, AdditionalTimeService.ADDITIONAL_TIME_SECONDS));
    }

    private SystemEvent mockGameResumedEvent() {
        return SystemEvent.of(TEST_GAME_ID, SystemEventType.GAME_RESUMED,
                SystemEventData.GameResumedData.of(1500L));
    }
}
