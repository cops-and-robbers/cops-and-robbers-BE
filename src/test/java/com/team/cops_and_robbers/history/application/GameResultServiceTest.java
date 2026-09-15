package com.team.cops_and_robbers.history.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.history.application.dto.command.GameResultCommand;
import com.team.cops_and_robbers.history.application.dto.result.GameResultResult;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import com.team.cops_and_robbers.history.exception.GameResultException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.CIRCLE_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.POLYGON_GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.ALIVE_ROBBER;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.WAITING_POLICE;
import static com.team.cops_and_robbers.common.fixture.GameResultFixture.POLICE_WIN_RESULT;
import static com.team.cops_and_robbers.common.fixture.GameResultFixture.POLICE_WIN_RESULT_POLYGON;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class GameResultServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameResultService gameResultService;

    @Spy
    private Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 6, 15, 0);

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_GAME_ID = 1L;
    private static final Long TEST_GAME_RESULT_ID = 1L;
    private static final Long POLICE_USER_ID = 101L;
    private static final Long ROBBER_USER_ID = 102L;

    private User police;
    private User robber;
    private Game inProgressGame;
    private GameArea gameArea;
    private GameResult openedResult;
    private GameParticipant policeParticipant;
    private GameParticipant robberParticipant;

    @BeforeEach
    void setUp() {
        police = USER("police");
        robber = USER("robber");
        inProgressGame = IN_PROGRESS_GAME();
        setId(police, POLICE_USER_ID);
        setId(robber, ROBBER_USER_ID);
        setId(inProgressGame, TEST_GAME_ID);

        gameArea = CIRCLE_GAME_AREA(inProgressGame);
        openedResult = GameResult.openSnapshot(inProgressGame, gameArea);
        setId(openedResult, TEST_GAME_RESULT_ID);

        policeParticipant = WAITING_POLICE(inProgressGame, police);
        robberParticipant = ALIVE_ROBBER(inProgressGame, robber);
    }

    @Nested
    @DisplayName("확인자료 열기")
    class OpenGameResult {

        @Test
        void CIRCLE_영역_게임이_시작되면_circle_필드가_채워진_결과가_열린다() {
            // given
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(gameArea);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            GameResult result = gameResultService.openGameResult(inProgressGame);

            // then
            assertThat(result.getAreaType()).isEqualTo(AreaType.CIRCLE);
            assertThat(result.getPlaygroundCenter()).isNotNull();
            assertThat(result.getPlaygroundRadiusInMeters()).isNotNull();
            assertThat(result.getPlaygroundPolygon()).isNull();
            assertThat(result.getJailCenter()).isEqualTo(gameArea.getJailCenter());
            assertThat(result.getJailRadiusInMeters()).isEqualTo(gameArea.getJailRadiusInMeters());
        }

        @Test
        void POLYGON_영역_게임이_시작되면_polygon_필드가_채워진_결과가_열린다() {
            // given
            GameArea polygonGameArea = POLYGON_GAME_AREA(inProgressGame);

            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(polygonGameArea);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            GameResult result = gameResultService.openGameResult(inProgressGame);

            // then
            assertThat(result.getAreaType()).isEqualTo(AreaType.POLYGON);
            assertThat(result.getPlaygroundPolygon()).isNotNull();
            assertThat(result.getPlaygroundCenter()).isNull();
            assertThat(result.getPlaygroundRadiusInMeters()).isNull();
            assertThat(result.getJailPolygon()).isEqualTo(polygonGameArea.getJailPolygon());
        }

        @Test
        void 게임이_시작되면_위치_수집_시작_시각과_공개_주기가_그대로_복사된다() {
            // given
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(gameArea);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            GameResult result = gameResultService.openGameResult(inProgressGame);

            // then
            assertThat(result.getStartedAt()).isEqualTo(inProgressGame.getStartedAt());
            assertThat(result.getLocationRevealIntervalMinutes())
                    .isEqualTo(inProgressGame.getLocationRevealIntervalMinutes());
        }

        @Test
        void 열린_결과는_아직_완료되지_않은_상태다() {
            // given
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(gameArea);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            GameResult result = gameResultService.openGameResult(inProgressGame);

            // then
            assertThat(result.isCompleted()).isFalse();
            assertThat(result.getEndReason()).isNull();
            assertThat(result.getWinnerTeam()).isNull();
            assertThat(result.getEndedAt()).isNull();
            assertThat(result.getDurationSeconds()).isNull();
        }

        /** 게임 중 퇴장자가 명단에 남으려면 시작 시점에 명단이 박혀 있어야 한다. */
        @SuppressWarnings("unchecked")
        @Test
        void 게임이_시작되면_시작_시점의_참가자_명단이_저장된다() {
            // given
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(gameArea);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));
            given(gameParticipantRepository.findAllByGameIdWithUser(TEST_GAME_ID))
                    .willReturn(List.of(policeParticipant, robberParticipant));

            // when
            gameResultService.openGameResult(inProgressGame);

            // then
            ArgumentCaptor<List<GameResultParticipant>> captor = ArgumentCaptor.forClass(List.class);
            then(gameResultParticipantRepository).should().saveAll(captor.capture());
            assertThat(captor.getValue()).extracting(GameResultParticipant::getUserId)
                    .containsExactlyInAnyOrder(POLICE_USER_ID, ROBBER_USER_ID);
            assertThat(captor.getValue()).allMatch(snapshot -> !snapshot.hasLeft());
        }
    }

    @Nested
    @DisplayName("게임 중 퇴장 기록")
    class RecordParticipantLeft {

        @Test
        void 게임_중_퇴장하면_명단에서_지우지_않고_퇴장_시각만_찍힌다() {
            // given
            GameResultParticipant snapshot =
                    GameResultParticipant.createSnapshot(openedResult, policeParticipant);

            given(gameResultRepository.findByGameIdAndEndReasonIsNull(TEST_GAME_ID))
                    .willReturn(Optional.of(openedResult));
            given(gameResultParticipantRepository
                    .findByGameResultIdAndUserIdAndLeftAtIsNull(TEST_GAME_RESULT_ID, POLICE_USER_ID))
                    .willReturn(Optional.of(snapshot));

            // when
            gameResultService.recordParticipantLeft(TEST_GAME_ID, POLICE_USER_ID);

            // then
            assertThat(snapshot.hasLeft()).isTrue();
            assertThat(snapshot.getLeftAt()).isEqualTo(NOW);
            assertThat(snapshot.getStatus()).isEqualTo(ParticipantStatus.POLICE_WAITING);
        }

        @Test
        void 열린_확인자료가_없으면_아무것도_하지_않는다() {
            // given
            given(gameResultRepository.findByGameIdAndEndReasonIsNull(TEST_GAME_ID))
                    .willReturn(Optional.empty());

            // when
            gameResultService.recordParticipantLeft(TEST_GAME_ID, POLICE_USER_ID);

            // then
            then(gameResultParticipantRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("게임 시작 후 입장 기록")
    class RecordParticipantJoined {

        @Test
        void 이벤트_게임에_중간_입장하면_명단에_추가된다() {
            // given
            given(gameResultRepository.findByGameIdAndEndReasonIsNull(TEST_GAME_ID))
                    .willReturn(Optional.of(openedResult));

            // when
            gameResultService.recordParticipantJoined(TEST_GAME_ID, policeParticipant);

            // then
            ArgumentCaptor<GameResultParticipant> captor = ArgumentCaptor.forClass(GameResultParticipant.class);
            then(gameResultParticipantRepository).should().save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(POLICE_USER_ID);
            assertThat(captor.getValue().hasLeft()).isFalse();
        }

        @Test
        void 열린_확인자료가_없으면_아무것도_하지_않는다() {
            // given
            given(gameResultRepository.findByGameIdAndEndReasonIsNull(TEST_GAME_ID))
                    .willReturn(Optional.empty());

            // when
            gameResultService.recordParticipantJoined(TEST_GAME_ID, policeParticipant);

            // then
            then(gameResultParticipantRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("게임 결과 기록")
    class RecordGameResult {

        @Test
        void 게임이_종료되면_열어_둔_결과에_승패와_집계가_채워진다() {
            // given
            given(gameResultRepository.findByGameIdAndEndReasonIsNull(TEST_GAME_ID))
                    .willReturn(Optional.of(openedResult));
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(2);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.ROBBER)).willReturn(3);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.JAILED)).willReturn(3);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            GameResult result = gameResultService.recordGameResult(inProgressGame, Team.POLICE, GameEndReason.ALL_ARRESTED);

            // then
            assertThat(result).isSameAs(openedResult);
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getWinnerTeam()).isEqualTo(Team.POLICE);
            assertThat(result.getEndReason()).isEqualTo(GameEndReason.ALL_ARRESTED);
            assertThat(result.getTotalPoliceCount()).isEqualTo(2);
            assertThat(result.getTotalRobberCount()).isEqualTo(3);
            assertThat(result.getArrestedRobberCount()).isEqualTo(3);
            assertThat(result.getEndedAt()).isEqualTo(NOW);
        }

        /** durationSeconds 와 endedAt 이 같은 시각에서 나와야 셋이 서로 어긋나지 않는다. */
        @Test
        void 수집_시작과_종료_시각의_차이가_기록된_진행_시간과_일치한다() {
            // given
            LocalDateTime startedAt = LocalDateTime.now().minusMinutes(40);
            Game longGame = IN_PROGRESS_GAME(startedAt);
            setId(longGame, TEST_GAME_ID);
            GameResult longOpenedResult = GameResult.openSnapshot(longGame, CIRCLE_GAME_AREA(longGame));
            setId(longOpenedResult, TEST_GAME_RESULT_ID);

            given(gameResultRepository.findByGameIdAndEndReasonIsNull(TEST_GAME_ID))
                    .willReturn(Optional.of(longOpenedResult));
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(2);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.ROBBER)).willReturn(3);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.JAILED)).willReturn(3);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            GameResult result = gameResultService.recordGameResult(longGame, Team.POLICE, GameEndReason.ALL_ARRESTED);

            // then
            assertThat(result.getStartedAt()).isEqualTo(startedAt);
            long betweenSeconds = Duration.between(result.getStartedAt(), result.getEndedAt()).getSeconds();
            assertThat(betweenSeconds).isEqualTo(result.getDurationSeconds().longValue());
        }

        /** 방 설정은 게임 사이에 바뀌므로, 지난 게임 기록이 새 설정으로 흔들리면 안 된다. */
        @Test
        void 게임_뒤에_방_설정이_바뀌어도_이미_저장된_공개_주기는_그대로다() {
            // given
            given(gameResultRepository.findByGameIdAndEndReasonIsNull(TEST_GAME_ID))
                    .willReturn(Optional.of(openedResult));
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(2);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.ROBBER)).willReturn(3);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.JAILED)).willReturn(3);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            GameResult result = gameResultService.recordGameResult(inProgressGame, Team.POLICE, GameEndReason.ALL_ARRESTED);
            Integer recordedInterval = result.getLocationRevealIntervalMinutes();

            // when
            inProgressGame.updateSettings(30, 1, 3, 10);

            // then
            assertThat(recordedInterval).isEqualTo(5);
            assertThat(result.getLocationRevealIntervalMinutes()).isEqualTo(5);
            assertThat(inProgressGame.getLocationRevealIntervalMinutes()).isEqualTo(1);
        }

        /** 감옥에 갔는지는 게임이 끝나야 알 수 있다. */
        @Test
        void 끝까지_남은_참가자는_종료_시점_상태로_갱신된다() {
            // given
            GameResultParticipant snapshot =
                    GameResultParticipant.createSnapshot(openedResult, robberParticipant);
            robberParticipant.updateStatus(ParticipantStatus.JAILED);

            given(gameResultRepository.findByGameIdAndEndReasonIsNull(TEST_GAME_ID))
                    .willReturn(Optional.of(openedResult));
            given(gameParticipantRepository.findAllByGameIdWithUser(TEST_GAME_ID)).willReturn(List.of(robberParticipant));
            given(gameResultParticipantRepository.findByGameResultId(TEST_GAME_RESULT_ID)).willReturn(List.of(snapshot));
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(0);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.ROBBER)).willReturn(1);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.JAILED)).willReturn(1);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            gameResultService.recordGameResult(inProgressGame, Team.POLICE, GameEndReason.ALL_ARRESTED);

            // then
            assertThat(snapshot.getStatus()).isEqualTo(ParticipantStatus.JAILED);
        }

        @Test
        void 퇴장한_참가자는_명단에_남고_상태도_덮이지_않는다() {
            // given
            GameResultParticipant leftSnapshot =
                    GameResultParticipant.createSnapshot(openedResult, robberParticipant);
            leftSnapshot.markLeft(LocalDateTime.now());

            given(gameResultRepository.findByGameIdAndEndReasonIsNull(TEST_GAME_ID))
                    .willReturn(Optional.of(openedResult));
            given(gameParticipantRepository.findAllByGameIdWithUser(TEST_GAME_ID)).willReturn(List.of());
            given(gameResultParticipantRepository.findByGameResultId(TEST_GAME_RESULT_ID)).willReturn(List.of(leftSnapshot));
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(0);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.ROBBER)).willReturn(0);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.JAILED)).willReturn(0);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            gameResultService.recordGameResult(inProgressGame, Team.POLICE, GameEndReason.ROBBER_FORFEITED);

            // then
            assertThat(leftSnapshot.getStatus()).isEqualTo(ParticipantStatus.ALIVE);
            assertThat(leftSnapshot.hasLeft()).isTrue();
        }

        @Test
        void 열린_확인자료가_없으면_그_자리에서_열고_채운다() {
            // given
            given(gameResultRepository.findByGameIdAndEndReasonIsNull(TEST_GAME_ID)).willReturn(Optional.empty());
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(gameArea);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(2);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.ROBBER)).willReturn(3);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.JAILED)).willReturn(3);
            given(gameResultRepository.save(any(GameResult.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            GameResult result = gameResultService.recordGameResult(inProgressGame, Team.POLICE, GameEndReason.ALL_ARRESTED);

            // then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getEndReason()).isEqualTo(GameEndReason.ALL_ARRESTED);
            assertThat(result.getAreaType()).isEqualTo(AreaType.CIRCLE);
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
        void 아직_끝나지_않은_게임_결과는_조회되지_않는다() {
            // given
            given(gameResultRepository.findById(TEST_GAME_RESULT_ID)).willReturn(Optional.of(openedResult));

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
