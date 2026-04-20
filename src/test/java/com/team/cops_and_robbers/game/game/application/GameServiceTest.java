package com.team.cops_and_robbers.game.game.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.domain.GameAreaDomainService;
import com.team.cops_and_robbers.game.game.application.dto.command.GameAreaUpdateCommand;
import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
import com.team.cops_and_robbers.game.game.application.dto.command.GameInfoCommand;
import com.team.cops_and_robbers.game.game.application.dto.command.GameSettingsUpdateCommand;
import com.team.cops_and_robbers.game.game.application.dto.result.GameAreaUpdateResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameSettingsUpdateResult;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.play.lobby.application.LobbyEventFactory;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameFixture.FINISHED_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.GUEST_PARTICIPANT;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.HOST_PARTICIPANT;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER_WITHOUT_TERMS;
import com.team.cops_and_robbers.user.exception.UserException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class GameServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameService gameService;

    @Mock
    private GameAreaDomainService gameAreaDomainService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LobbyEventFactory lobbyEventFactory;

    @Mock
    private LobbyEvent lobbyEvent;

    private static final Long TEST_HOST_ID = 1L;
    private static final Long TEST_GAME_ID = 1L;

    private User host;
    private Game game;
    private Game inProgressGame;
    private Game finishedGame;

    @BeforeEach
    void setUp() {
        host = USER();
        game = WAITING_GAME();
        inProgressGame = IN_PROGRESS_GAME();
        finishedGame = FINISHED_GAME();
        setId(host, TEST_HOST_ID);
        setId(game, TEST_GAME_ID);
        setId(inProgressGame, TEST_GAME_ID);
        setId(finishedGame, TEST_GAME_ID);
    }

    @Nested
    @DisplayName("게임 방 생성")
    class CreateGame {

        @Test
        void 게임_생성에_성공하면_결과를_반환한다() {
            // given
            GameCreateCommand command = createGameCommand(host.getId());

            given(userRepository.getByUserId(host.getId())).willReturn(host);
            given(gameParticipantRepository.existsActiveGameByUserId(host.getId())).willReturn(false);
            given(gameRepository.save(any(Game.class))).willReturn(game);
            given(gameRepository.existsByInviteCode(anyString())).willReturn(false);

            // when
            gameService.createGame(command);

            // then
            then(gameAreaDomainService).should().validateAreaContainment(anyDouble(), anyDouble(), anyInt(), anyDouble(), anyDouble(), anyInt());
            then(gameRepository).should().save(any(Game.class));
            then(gameAreaRepository).should().save(any(GameArea.class));
            then(gameParticipantRepository).should().save(any(GameParticipant.class));
        }

        @Test
        void 이미_참여_중인_활성_게임이_있는_유저라면_예외가_발생한다() {
            // given
            GameCreateCommand command = createGameCommand(host.getId());

            given(userRepository.getByUserId(host.getId())).willReturn(host);
            given(gameParticipantRepository.existsActiveGameByUserId(host.getId())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> gameService.createGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.ALREADY_PARTICIPATING.getDetail());

            then(gameRepository).should(never()).save(any(Game.class));
            then(gameAreaRepository).should(never()).save(any(GameArea.class));
            then(gameParticipantRepository).should(never()).save(any(GameParticipant.class));
        }

        @Test
        void 필수_약관에_미동의한_유저라면_예외가_발생한다() {
            // given
            User unagreedHost = USER_WITHOUT_TERMS("unagreedHost");
            setId(unagreedHost, TEST_HOST_ID);
            GameCreateCommand command = createGameCommand(unagreedHost.getId());

            given(userRepository.getByUserId(unagreedHost.getId())).willReturn(unagreedHost);

            // when & then
            assertThatThrownBy(() -> gameService.createGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(UserException.REQUIRED_TERMS_NOT_AGREED.getDetail());

            then(gameRepository).should(never()).save(any(Game.class));
            then(gameParticipantRepository).should(never()).save(any(GameParticipant.class));
        }
    }

    @Nested
    @DisplayName("게임 기본 설정 조회")
    class GetGameInfo {

        @Test
        void WAITING_상태_게임의_설정_조회에_성공한다() {
            // given
            GameParticipant participant = HOST_PARTICIPANT(game, host);
            GameInfoCommand command = GameInfoCommand.of(TEST_HOST_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_HOST_ID)).willReturn(participant);

            // when
            gameService.getGameInfo(command);

            // then
            then(gameRepository).should().getByGameId(TEST_GAME_ID);
            then(gameParticipantRepository).should().getByGameIdAndUserId(TEST_GAME_ID, TEST_HOST_ID);
        }

        @Test
        void IN_PROGRESS_상태_게임의_설정_조회에_성공한다() {
            // given
            GameParticipant participant = HOST_PARTICIPANT(inProgressGame, host);
            GameInfoCommand command = GameInfoCommand.of(TEST_HOST_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(inProgressGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_HOST_ID)).willReturn(participant);

            // when
            gameService.getGameInfo(command);

            // then
            then(gameRepository).should().getByGameId(TEST_GAME_ID);
            then(gameParticipantRepository).should().getByGameIdAndUserId(TEST_GAME_ID, TEST_HOST_ID);
        }

        @Test
        void 게임이_비활성_상태이면_예외가_발생한다() {
            // given
            GameInfoCommand command = GameInfoCommand.of(TEST_HOST_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(finishedGame);

            // when & then
            assertThatThrownBy(() -> gameService.getGameInfo(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameException.GAME_NOT_ACTIVE.getDetail());
        }

        @Test
        void 해당_게임의_참가자가_아니면_예외가_발생한다() {
            // given
            GameInfoCommand command = GameInfoCommand.of(TEST_HOST_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_HOST_ID))
                    .willThrow(new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> gameService.getGameInfo(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.PARTICIPANT_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("게임 영역 수정")
    class UpdateGameArea {

        private GameAreaUpdateCommand command;
        private GameArea gameArea;

        @BeforeEach
        void setUp() {
            command = new GameAreaUpdateCommand(
                    TEST_GAME_ID,
                    TEST_HOST_ID,
                    37.4979,
                    127.0276,
                    1000,
                    37.4979,
                    127.0276,
                    100
            );
            gameArea = GAME_AREA(game);
        }

        @Test
        void 영역_수정에_성공하면_결과를_반환한다() {
            // given
            GameParticipant hostParticipant = HOST_PARTICIPANT(game, host);
            given(gameParticipantRepository.getByGameIdAndUserIdWithGame(TEST_GAME_ID, TEST_HOST_ID)).willReturn(hostParticipant);
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(gameArea);
            given(lobbyEventFactory.createAreaUpdatedEvent(any(), any())).willReturn(lobbyEvent);

            // when
            GameAreaUpdateResult result = gameService.updateGameArea(command);

            // then
            then(gameAreaDomainService).should().validateAreaContainment(anyDouble(), anyDouble(), anyInt(), anyDouble(), anyDouble(), anyInt());
            then(eventPublisher).should().publishEvent(lobbyEvent);
            assertThat(result).isNotNull();
        }

        @Test
        void 방장이_아닌_참여자가_요청하면_예외가_발생한다() {
            // given
            GameParticipant guest = GUEST_PARTICIPANT(game, host);
            given(gameParticipantRepository.getByGameIdAndUserIdWithGame(TEST_GAME_ID, TEST_HOST_ID)).willReturn(guest);

            // when & then
            assertThatThrownBy(() -> gameService.updateGameArea(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.NOT_HOST.getDetail());

            then(gameAreaRepository).should(never()).getByGameId(any());
        }

        @Test
        void 게임이_대기_상태가_아니라면_예외가_발생한다() {
            // given
            Game inProgressGame = IN_PROGRESS_GAME();
            GameParticipant hostParticipant = HOST_PARTICIPANT(inProgressGame, host);
            given(gameParticipantRepository.getByGameIdAndUserIdWithGame(TEST_GAME_ID, TEST_HOST_ID)).willReturn(hostParticipant);

            // when & then
            assertThatThrownBy(() -> gameService.updateGameArea(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameException.GAME_NOT_WAITING.getDetail());

            then(gameAreaRepository).should(never()).getByGameId(any());
        }
    }

    @Nested
    @DisplayName("게임 설정 수정")
    class UpdateGameSettings {

        private GameSettingsUpdateCommand command;

        @BeforeEach
        void setUp() {
            command = new GameSettingsUpdateCommand(
                    TEST_GAME_ID,
                    TEST_HOST_ID,
                    60,
                    10,
                    5,
                    20
            );
        }

        @Test
        void 설정_수정에_성공하면_결과를_반환한다() {
            // given
            GameParticipant hostParticipant = HOST_PARTICIPANT(game, host);
            given(gameParticipantRepository.getByGameIdAndUserIdWithGame(TEST_GAME_ID, TEST_HOST_ID)).willReturn(hostParticipant);
            given(lobbyEventFactory.createSettingsUpdatedEvent(any(), any())).willReturn(lobbyEvent);

            // when
            GameSettingsUpdateResult result = gameService.updateGameSettings(command);

            // then
            then(eventPublisher).should().publishEvent(lobbyEvent);
            assertThat(result.roundDurationMinutes()).isEqualTo(command.roundDurationMinutes());
            assertThat(result.locationRevealIntervalMinutes()).isEqualTo(command.locationRevealIntervalMinutes());
            assertThat(result.policeWaitMinutes()).isEqualTo(command.policeWaitMinutes());
            assertThat(result.maxParticipants()).isEqualTo(command.maxParticipants());
        }

        @Test
        void 방장이_아닌_참여자가_요청하면_예외가_발생한다() {
            // given
            GameParticipant guest = GUEST_PARTICIPANT(game, host);
            given(gameParticipantRepository.getByGameIdAndUserIdWithGame(TEST_GAME_ID, TEST_HOST_ID)).willReturn(guest);

            // when & then
            assertThatThrownBy(() -> gameService.updateGameSettings(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.NOT_HOST.getDetail());
        }

        @Test
        void 게임이_대기_상태가_아니라면_예외가_발생한다() {
            // given
            Game inProgressGame = IN_PROGRESS_GAME();
            GameParticipant hostParticipant = HOST_PARTICIPANT(inProgressGame, host);
            given(gameParticipantRepository.getByGameIdAndUserIdWithGame(TEST_GAME_ID, TEST_HOST_ID)).willReturn(hostParticipant);

            // when & then
            assertThatThrownBy(() -> gameService.updateGameSettings(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameException.GAME_NOT_WAITING.getDetail());
        }
    }

    private GameCreateCommand createGameCommand(Long hostId) {
        return new GameCreateCommand(
                hostId,
                37.5665, 126.978, 1000, // playgroundLat, playgroundLon, playgroundRadius
                37.5665, 126.978, 100,  // jailLat, jailLon, jailRadius
                30, // roundDurationMinutes
                5,  // locationRevealIntervalMinutes
                3,  // policeWaitMinutes
                10  // maxParticipants
        );
    }
}
