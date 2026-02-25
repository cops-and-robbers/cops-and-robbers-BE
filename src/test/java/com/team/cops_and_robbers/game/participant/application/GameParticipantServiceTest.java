package com.team.cops_and_robbers.game.participant.application;


import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameJoinCommand;
import com.team.cops_and_robbers.game.participant.application.dto.command.GameLeaveCommand;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameJoinResult;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameLeaveResult;
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

import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.ALIVE_POLICE;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.GUEST_PARTICIPANT;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.HOST_PARTICIPANT;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

class GameParticipantServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameParticipantService gameParticipantService;

    @Mock
    private LobbyEventFactory lobbyEventFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LobbyEvent lobbyEvent;

    private User user;
    private User user2;
    private Game waitingGame;
    private GameParticipant guestParticipant;
    private GameParticipant hostParticipant;
    private  String INVITE_CODE = "ABCDEF";

    @BeforeEach
    void setUp() {
        user = USER();
        user2 = USER();
        waitingGame = WAITING_GAME(INVITE_CODE);
        guestParticipant = GUEST_PARTICIPANT(waitingGame, user);
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

        @Test
        void 참가자의_상태가_WAITING이_아니면_예외가_발생한다() {
            // given
            GameParticipant aliveParticipant = ALIVE_POLICE(waitingGame, user);
            GameLeaveCommand command = createGameLeaveCommand(user.getId(), waitingGame.getId());

            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId()))
                    .willReturn(aliveParticipant);

            // when & then
            assertThatThrownBy(() -> gameParticipantService.leaveGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.CANNOT_LEAVE_DURING_GAME.getDetail());
        }
    }

    private GameLeaveCommand createGameLeaveCommand(Long userId,  Long gameId) {
        return new GameLeaveCommand(userId, gameId);
    }
}
