package com.team.cops_and_robbers.game.participant.application;


import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameJoinCommand;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameLeaveCommand;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameParticipantListCommand;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameJoinResult;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameLeaveResult;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameParticipantListResult;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.lobby.application.LobbyEventFactory;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import com.team.cops_and_robbers.play.system.application.GameTerminationService;
import com.team.cops_and_robbers.play.system.application.SystemEventFactory;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.*;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER_WITHOUT_TERMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class GameParticipantServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameParticipantService gameParticipantService;

    @Mock
    private LobbyEventFactory lobbyEventFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private InGameParticipantCacheRepository inGameParticipantCacheRepository;

    @Mock
    private SystemEventFactory systemEventFactory;

    @Mock
    private GameTerminationService gameTerminationService;

    @Mock
    private LobbyEvent lobbyEvent;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_GAME_ID = 1L;

    private User user;
    private User user2;
    private Game waitingGame;
    private Game inProgressGame;
    private GameParticipant guestParticipant;
    private GameParticipant hostParticipant;
    private GameParticipant policeParticipant;
    private GameParticipant robberParticipant;
    private String INVITE_CODE = "ABCDEF";

    @BeforeEach
    void setUp() {
        user = USER();
        user2 = USER();
        waitingGame = WAITING_GAME(INVITE_CODE);
        inProgressGame = IN_PROGRESS_GAME();
        setId(user, TEST_USER_ID);
        setId(waitingGame, TEST_GAME_ID);
        setId(inProgressGame, TEST_GAME_ID);
        guestParticipant = GUEST_PARTICIPANT(waitingGame, user);
        policeParticipant = WAITING_POLICE(inProgressGame, user);
        robberParticipant = ALIVE_ROBBER(inProgressGame, user2);
    }

    @Nested
    @DisplayName("게임 참여")
    class joinGame {

        @Test
        void 게임_참여에_성공하면_결과를_반환하고_이벤트를_발행한다() {
            // given
            GameJoinCommand command = createGameJoinCommand(user.getId(), INVITE_CODE);

            given(gameRepository.getByInviteCode(INVITE_CODE)).willReturn(waitingGame);
            given(gameParticipantRepository.existsActiveGameByUserId(user.getId())).willReturn(false);
            given(gameParticipantRepository.countByGameId(waitingGame.getId())).willReturn(0);
            given(userRepository.getByUserId(user.getId())).willReturn(user);
            given(gameParticipantRepository.save(any(GameParticipant.class))).willReturn(guestParticipant);
            given(lobbyEventFactory.createEnterEvent(any(), any(GameParticipant.class), anyInt(), anyInt())).willReturn(lobbyEvent);

            // when
            GameJoinResult result = gameParticipantService.joinGame(command);

            // then
            assertThat(result).isEqualTo(GameJoinResult.from(guestParticipant));
            then(eventPublisher).should().publishEvent(lobbyEvent);
        }

        @Test
        void 이미_참여_중인_활성_게임이_있는_유저라면_예외가_발생한다() {
            // given
            GameJoinCommand command = createGameJoinCommand(user.getId(), INVITE_CODE);

            given(gameRepository.getByInviteCode(INVITE_CODE)).willReturn(waitingGame);
            given(gameParticipantRepository.existsActiveGameByUserId(user.getId())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> gameParticipantService.joinGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.ALREADY_PARTICIPATING.getDetail());
        }

        @Test
        void 게임이_WAITING_상태가_아니라면_예외가_발생한다() {
            // given
            Game inProgressGame = IN_PROGRESS_GAME();
            setId(inProgressGame, 1L);

            GameJoinCommand command = createGameJoinCommand(user.getId(), INVITE_CODE);

            given(gameRepository.getByInviteCode(INVITE_CODE)).willReturn(inProgressGame);
            given(gameParticipantRepository.existsActiveGameByUserId(user.getId())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> gameParticipantService.joinGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.GAME_ALREADY_STARTED.getDetail());
        }

        @Test
        void 필수_약관에_미동의한_유저라면_예외가_발생한다() {
            // given
            User unagreedUser = USER_WITHOUT_TERMS("unagreedUser");
            setId(unagreedUser, TEST_USER_ID);
            GameJoinCommand command = createGameJoinCommand(unagreedUser.getId(), INVITE_CODE);

            given(gameRepository.getByInviteCode(INVITE_CODE)).willReturn(waitingGame);
            given(gameParticipantRepository.existsActiveGameByUserId(unagreedUser.getId())).willReturn(false);
            given(userRepository.getByUserId(unagreedUser.getId())).willReturn(unagreedUser);

            // when & then
            assertThatThrownBy(() -> gameParticipantService.joinGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(UserException.REQUIRED_TERMS_NOT_AGREED.getDetail());

            then(gameParticipantRepository).should(never()).save(any(GameParticipant.class));
        }

        @Test
        void 현재_인원이_게임_최대_인원을_초과하면_예외가_발생한다() {
            // given
            GameJoinCommand command = createGameJoinCommand(user.getId(), INVITE_CODE);
            int maxParticipants = waitingGame.getMaxParticipants();

            given(gameRepository.getByInviteCode(INVITE_CODE)).willReturn(waitingGame);
            given(gameParticipantRepository.existsActiveGameByUserId(user.getId())).willReturn(false);
            given(gameParticipantRepository.countByGameId(waitingGame.getId())).willReturn(maxParticipants);

            // when & then
            assertThatThrownBy(() -> gameParticipantService.joinGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.GAME_FULL.getDetail());
        }
    }

    private GameJoinCommand createGameJoinCommand(Long userId, String inviteCode) {
        return new GameJoinCommand(userId, inviteCode);
    }

    @Nested
    @DisplayName("게임 퇴장")
    class leaveGame {
        @Test
        void 마지막_사람이_퇴장에_성공할_경우_방을_삭제한다() {
            // given
            GameLeaveCommand command = createGameLeaveCommand(user.getId(), waitingGame.getId());

            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(guestParticipant);
            given(gameParticipantRepository.countByGameId(waitingGame.getId())).willReturn(0);

            // when
            GameLeaveResult result = gameParticipantService.leaveGame(command);

            // then
            assertThat(result).isEqualTo(GameLeaveResult.from(user.getId(), 0));
            then(gameParticipantRepository).should().delete(guestParticipant);
            then(gameAreaRepository).should().deleteByGameId(waitingGame.getId());
            then(gameRepository).should().deleteById(waitingGame.getId());
        }

        @Test
        void 방장이_퇴장에_성공할_경우_다음_들어온_사람에게_방장을_위임하고_이벤트를_발행한다() {
            // given
            hostParticipant = HOST_PARTICIPANT(waitingGame, user2);
            GameLeaveCommand command = createGameLeaveCommand(user2.getId(), waitingGame.getId());

            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user2.getId())).willReturn(hostParticipant);
            given(gameParticipantRepository.countByGameId(waitingGame.getId())).willReturn(1);
            given(gameParticipantRepository.getNextParticipant(waitingGame.getId())).willReturn(guestParticipant);
            given(lobbyEventFactory.createHostChangedEvent(any(), any(GameParticipant.class))).willReturn(lobbyEvent);
            given(lobbyEventFactory.createExitEvent(any(), any(), anyInt(), anyInt())).willReturn(lobbyEvent);

            // when
            GameLeaveResult result = gameParticipantService.leaveGame(command);

            // then
            assertThat(guestParticipant.isHost()).isTrue();
            assertThat(result).isEqualTo(GameLeaveResult.from(user2.getId(), 1));
            then(gameParticipantRepository).should().delete(hostParticipant);
            then(eventPublisher).should(times(2)).publishEvent(lobbyEvent);
        }

    }

    @Nested
    @DisplayName("인게임 중도 퇴장")
    class leaveFromInGame {

        @Test
        void 경찰이_퇴장하고_경찰이_남아있으면_PLAYER_LEFT_이벤트를_발행한다() {
            // given
            GameLeaveCommand command = createGameLeaveCommand(user.getId(), TEST_GAME_ID);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, user.getId())).willReturn(policeParticipant);
            given(gameParticipantRepository.countByGameId(TEST_GAME_ID)).willReturn(1);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(1);
            given(systemEventFactory.createPlayerLeftEvent(any(), any())).willReturn(mock(SystemEvent.class));

            // when
            gameParticipantService.leaveGame(command);

            // then
            then(inGameParticipantCacheRepository).should().deleteByParticipantId(any(), any());
            then(gameTerminationService).should(never()).endGameByPoliceForfeited(any());
            then(eventPublisher).should().publishEvent(any(SystemEvent.class));
        }

        @Test
        void 마지막_경찰이_퇴장하면_POLICE_FORFEITED로_게임이_종료된다() {
            // given
            GameLeaveCommand command = createGameLeaveCommand(user.getId(), TEST_GAME_ID);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, user.getId())).willReturn(policeParticipant);
            given(gameParticipantRepository.countByGameId(TEST_GAME_ID)).willReturn(0);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(0);
            given(systemEventFactory.createPlayerLeftEvent(any(), any())).willReturn(mock(SystemEvent.class));

            // when
            gameParticipantService.leaveGame(command);

            // then
            then(gameTerminationService).should().endGameByPoliceForfeited(TEST_GAME_ID);
            then(eventPublisher).should(never()).publishEvent(any(SystemEvent.class));
        }

        @Test
        void ALIVE_도둑이_퇴장하고_생존_도둑이_남아있으면_PLAYER_LEFT_이벤트를_발행한다() {
            // given
            GameLeaveCommand command = createGameLeaveCommand(user2.getId(), TEST_GAME_ID);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, user2.getId())).willReturn(robberParticipant);
            given(gameParticipantRepository.countByGameId(TEST_GAME_ID)).willReturn(1);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.ALIVE)).willReturn(1);
            given(systemEventFactory.createPlayerLeftEvent(any(), any())).willReturn(mock(SystemEvent.class));

            // when
            gameParticipantService.leaveGame(command);

            // then
            then(gameTerminationService).should(never()).endGameByRobberForfeited(any());
            then(eventPublisher).should().publishEvent(any(SystemEvent.class));
        }

        @Test
        void 마지막_생존_도둑이_퇴장하면_ROBBER_FORFEITED로_게임이_종료된다() {
            // given
            GameLeaveCommand command = createGameLeaveCommand(user2.getId(), TEST_GAME_ID);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, user2.getId())).willReturn(robberParticipant);
            given(gameParticipantRepository.countByGameId(TEST_GAME_ID)).willReturn(0);
            given(gameParticipantRepository.countByGameIdAndRobberStatus(TEST_GAME_ID, ParticipantStatus.ALIVE)).willReturn(0);
            given(systemEventFactory.createPlayerLeftEvent(any(), any())).willReturn(mock(SystemEvent.class));

            // when
            gameParticipantService.leaveGame(command);

            // then
            then(gameTerminationService).should().endGameByRobberForfeited(TEST_GAME_ID);
            then(eventPublisher).should(never()).publishEvent(any(SystemEvent.class));
        }

        @Test
        void JAILED_도둑이_퇴장하면_forfeit_조건_체크_없이_PLAYER_LEFT_이벤트를_발행한다() {
            // given
            GameParticipant jailedRobber = JAILED_ROBBER(inProgressGame, user);
            GameLeaveCommand command = createGameLeaveCommand(user.getId(), TEST_GAME_ID);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, user.getId())).willReturn(jailedRobber);
            given(gameParticipantRepository.countByGameId(TEST_GAME_ID)).willReturn(1);
            given(systemEventFactory.createPlayerLeftEvent(any(), any())).willReturn(mock(SystemEvent.class));

            // when
            gameParticipantService.leaveGame(command);

            // then
            then(gameParticipantRepository).should(never()).countByGameIdAndTeam(any(), any());
            then(gameParticipantRepository).should(never()).countByGameIdAndRobberStatus(any(), any());
            then(gameTerminationService).should(never()).endGameByPoliceForfeited(any());
            then(gameTerminationService).should(never()).endGameByRobberForfeited(any());
            then(eventPublisher).should().publishEvent(any(SystemEvent.class));
        }

        @Test
        void 방장이_인게임에서_퇴장하면_다음_참가자에게_방장이_위임된다() {
            // given
            GameParticipant hostPolice = HOST_WAITING_POLICE(inProgressGame, user);
            GameLeaveCommand command = createGameLeaveCommand(user.getId(), TEST_GAME_ID);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, user.getId())).willReturn(hostPolice);
            given(gameParticipantRepository.countByGameId(TEST_GAME_ID)).willReturn(1);
            given(gameParticipantRepository.countByGameIdAndTeam(TEST_GAME_ID, Team.POLICE)).willReturn(1);
            given(gameParticipantRepository.getNextParticipant(TEST_GAME_ID)).willReturn(robberParticipant);
            given(lobbyEventFactory.createHostChangedEvent(any(), any())).willReturn(lobbyEvent);
            given(systemEventFactory.createPlayerLeftEvent(any(), any())).willReturn(mock(SystemEvent.class));

            // when
            gameParticipantService.leaveGame(command);

            // then
            assertThat(robberParticipant.isHost()).isTrue();
            then(eventPublisher).should().publishEvent(lobbyEvent);
            then(eventPublisher).should().publishEvent(any(SystemEvent.class));
        }
    }

    private GameLeaveCommand createGameLeaveCommand(Long userId,  Long gameId) {
        return new GameLeaveCommand(userId, gameId);
    }

    @Nested
    @DisplayName("게임 참가자 목록 조회")
    class GetParticipantList {

        @Test
        void IN_PROGRESS_게임의_참가자_목록_조회에_성공한다() {
            // given
            GameParticipantListCommand command = GameParticipantListCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(inProgressGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID)).willReturn(policeParticipant);
            given(gameParticipantRepository.findAllByGameIdWithUser(TEST_GAME_ID)).willReturn(List.of(policeParticipant, robberParticipant));

            // when
            GameParticipantListResult result = gameParticipantService.getParticipantList(command);

            // then
            assertThat(result.police()).hasSize(1);
            assertThat(result.robbers()).hasSize(1);
        }

        @Test
        void 게임이_IN_PROGRESS_상태가_아니면_예외가_발생한다() {
            // given
            GameParticipantListCommand command = GameParticipantListCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(waitingGame);

            // when & then
            assertThatThrownBy(() -> gameParticipantService.getParticipantList(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameException.GAME_NOT_IN_PROGRESS.getDetail());
        }

        @Test
        void 해당_게임의_참가자가_아니면_예외가_발생한다() {
            // given
            GameParticipantListCommand command = GameParticipantListCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(inProgressGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID))
                    .willThrow(new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> gameParticipantService.getParticipantList(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.PARTICIPANT_NOT_FOUND.getDetail());
        }
    }
}
