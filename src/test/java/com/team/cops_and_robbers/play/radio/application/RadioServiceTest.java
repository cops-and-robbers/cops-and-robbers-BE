package com.team.cops_and_robbers.play.radio.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.history.application.GameResultService;
import com.team.cops_and_robbers.play.radio.application.dto.command.RadioTokenCommand;
import com.team.cops_and_robbers.play.radio.application.dto.result.RadioTokenResult;
import com.team.cops_and_robbers.play.radio.exception.RadioException;
import com.team.cops_and_robbers.play.radio.infrastructure.RadioToken;
import com.team.cops_and_robbers.play.radio.infrastructure.RadioTokenProvider;
import com.team.cops_and_robbers.play.radio.repository.RadioOverloadRepository;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.GameFixture.EVENT_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.FINISHED_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_TAG_GAME;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.ALIVE_CHASER;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.ALIVE_RUNNER;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.WAITING_CHASER;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class RadioServiceTest extends ServiceUnitTest {

    @InjectMocks
    private RadioService radioService;

    @Mock
    private GameResultService gameResultService;

    @Mock
    private RadioTokenProvider radioTokenProvider;

    @Mock
    private RadioOverloadRepository radioOverloadRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 9, 17, 0);

    @Spy
    private Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_GAME_ID = 1L;
    private static final Long TEST_PARTICIPANT_ID = 101L;
    private static final Duration TOKEN_TTL = Duration.ofMinutes(2);

    private User user;
    private Game inProgressGame;
    private RadioTokenCommand command;

    @BeforeEach
    void setUp() {
        user = USER();
        inProgressGame = IN_PROGRESS_GAME();
        setId(user, TEST_USER_ID);
        setId(inProgressGame, TEST_GAME_ID);
        command = RadioTokenCommand.of(TEST_GAME_ID, TEST_USER_ID);
    }

    @Nested
    @DisplayName("무전 토큰 발급")
    class IssueToken {

        @Test
        void 경찰이면_무전_사용을_기록하고_토큰을_발급한다() {
            // given
            givenGameAndParticipant(inProgressGame, createParticipant(ALIVE_CHASER(inProgressGame, user)));
            givenToken();

            // when
            RadioTokenResult result = radioService.issueToken(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.url()).isEqualTo("wss://test.livekit.cloud");
                softly.assertThat(result.room()).isEqualTo("dev-game-1-chaser");
                softly.assertThat(result.identity()).isEqualTo("101");
                softly.assertThat(result.token()).isEqualTo("signed-token");
            });
            then(gameResultService).should().recordRadioUsage(TEST_GAME_ID, TEST_USER_ID, TOKEN_TTL);
        }

        @Test
        void 술래잡기_게임의_술래도_토큰을_발급받는다() {
            // given
            Game tagGame = IN_PROGRESS_TAG_GAME();
            setId(tagGame, TEST_GAME_ID);
            givenGameAndParticipant(tagGame, createParticipant(ALIVE_CHASER(tagGame, user)));
            givenToken();

            // when
            RadioTokenResult result = radioService.issueToken(command);

            // then
            assertSoftly(softly -> softly.assertThat(result.token()).isEqualTo("signed-token"));
        }

        @Test
        void 게임이_없으면_예외가_발생한다() {
            // given
            given(gameRepository.getByGameId(TEST_GAME_ID))
                    .willThrow(new ApplicationException(GameException.GAME_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> radioService.issueToken(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameException.GAME_NOT_FOUND.getDetail());
        }

        @Test
        void 해당_게임의_참가자가_아니면_예외가_발생한다() {
            // given
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(inProgressGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID))
                    .willThrow(new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> radioService.issueToken(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.PARTICIPANT_NOT_FOUND.getDetail());
        }

        @Test
        void 진행_중인_게임이_아니면_예외가_발생한다() {
            // given
            Game finishedGame = FINISHED_GAME();
            setId(finishedGame, TEST_GAME_ID);
            givenGameAndParticipant(finishedGame, createParticipant(ALIVE_CHASER(finishedGame, user)));

            // when & then
            assertThatThrownBy(() -> radioService.issueToken(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameException.GAME_NOT_IN_PROGRESS.getDetail());
        }

        @Test
        void 이벤트_게임이면_예외가_발생한다() {
            // given
            Game eventGame = EVENT_GAME();
            setId(eventGame, TEST_GAME_ID);
            givenGameAndParticipant(eventGame, createParticipant(ALIVE_CHASER(eventGame, user)));

            // when & then
            assertThatThrownBy(() -> radioService.issueToken(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(RadioException.RADIO_NOT_ALLOWED_IN_EVENT_GAME.getDetail());
        }

        @Test
        void 도둑이면_예외가_발생한다() {
            // given
            givenGameAndParticipant(inProgressGame, createParticipant(ALIVE_RUNNER(inProgressGame, user)));

            // when & then
            assertThatThrownBy(() -> radioService.issueToken(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(RadioException.ONLY_CHASER_CAN_USE_RADIO.getDetail());
        }

        @Test
        void 경찰_대기_시간_중이면_예외가_발생한다() {
            // given
            givenGameAndParticipant(inProgressGame, createParticipant(WAITING_CHASER(inProgressGame, user)));

            // when & then
            assertThatThrownBy(() -> radioService.issueToken(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(RadioException.RADIO_CHASER_WAITING_TIME.getDetail());
        }
    }

    @Nested
    @DisplayName("무전 서버 과부하")
    class Overload {

        @Test
        void 과부하_중_신규_입장이면_예외가_발생한다() {
            // given
            givenGameAndParticipant(inProgressGame, createParticipant(ALIVE_CHASER(inProgressGame, user)));
            given(radioOverloadRepository.isActive()).willReturn(true);
            given(radioTokenProvider.tokenTtl()).willReturn(TOKEN_TTL);
            given(gameResultService.findRadioLastIssuedAt(TEST_GAME_ID, TEST_USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> radioService.issueToken(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(RadioException.RADIO_SERVER_BUSY.getDetail());
            then(gameResultService).should(never()).recordRadioUsage(any(), any(), any());
        }

        @Test
        void 과부하_중이라도_토큰_수명과_여유_시간_안의_재발급이면_토큰을_발급한다() {
            // given
            givenGameAndParticipant(inProgressGame, createParticipant(ALIVE_CHASER(inProgressGame, user)));
            given(radioOverloadRepository.isActive()).willReturn(true);
            given(gameResultService.findRadioLastIssuedAt(TEST_GAME_ID, TEST_USER_ID))
                    .willReturn(Optional.of(NOW.minusMinutes(2).minusSeconds(50)));
            givenToken();

            // when
            RadioTokenResult result = radioService.issueToken(command);

            // then
            assertSoftly(softly -> softly.assertThat(result.token()).isEqualTo("signed-token"));
        }

        @Test
        void 과부하_중_마지막_발급이_여유_시간을_넘었으면_예외가_발생한다() {
            // given
            givenGameAndParticipant(inProgressGame, createParticipant(ALIVE_CHASER(inProgressGame, user)));
            given(radioOverloadRepository.isActive()).willReturn(true);
            given(radioTokenProvider.tokenTtl()).willReturn(TOKEN_TTL);
            given(gameResultService.findRadioLastIssuedAt(TEST_GAME_ID, TEST_USER_ID))
                    .willReturn(Optional.of(NOW.minusMinutes(3).minusSeconds(1)));

            // when & then
            assertThatThrownBy(() -> radioService.issueToken(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(RadioException.RADIO_SERVER_BUSY.getDetail());
        }

        @Test
        void 과부하_플래그를_읽지_못하면_토큰을_발급한다() {
            // given
            givenGameAndParticipant(inProgressGame, createParticipant(ALIVE_CHASER(inProgressGame, user)));
            given(radioOverloadRepository.isActive()).willThrow(new RedisConnectionFailureException("down"));
            givenToken();

            // when
            RadioTokenResult result = radioService.issueToken(command);

            // then
            assertSoftly(softly -> softly.assertThat(result.token()).isEqualTo("signed-token"));
        }

        @Test
        void 과부하_신호를_받으면_10분짜리_플래그를_저장한다() {
            // when
            radioService.startOverload();

            // then
            then(radioOverloadRepository).should().activate(Duration.ofMinutes(10));
        }

        @Test
        void 해제_신호를_받으면_플래그를_지운다() {
            // when
            radioService.endOverload();

            // then
            then(radioOverloadRepository).should().deactivate();
        }
    }

    private GameParticipant createParticipant(GameParticipant participant) {
        setId(participant, TEST_PARTICIPANT_ID);
        return participant;
    }

    private void givenGameAndParticipant(Game game, GameParticipant participant) {
        given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
        given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID)).willReturn(participant);
    }

    private void givenToken() {
        given(radioTokenProvider.tokenTtl()).willReturn(TOKEN_TTL);
        given(radioTokenProvider.createJoinToken(eq("game-1-chaser"), eq(TEST_PARTICIPANT_ID), anyString()))
                .willReturn(new RadioToken("dev-game-1-chaser", "signed-token", NOW.plus(TOKEN_TTL)));
        given(radioTokenProvider.serverUrl()).willReturn("wss://test.livekit.cloud");
    }
}
