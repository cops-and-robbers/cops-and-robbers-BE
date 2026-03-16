package com.team.cops_and_robbers.play.lobby.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.play.lobby.application.dto.command.GameStartCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.KickCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.LobbyInfoCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.ReadyUpdateCommand;
import com.team.cops_and_robbers.play.lobby.application.dto.command.TeamChangeCommand;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.ALIVE_ROBBER;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.GUEST_PARTICIPANT;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.HOST_PARTICIPANT;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class LobbyServiceTest extends ServiceUnitTest {

    @InjectMocks
    private LobbyService lobbyService;

    @Spy
    private Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

    @Mock
    private LobbyEventFactory lobbyEventFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LobbyEvent lobbyEvent;

    private User user;
    private Game waitingGame;

    @BeforeEach
    void setUp() {
        user = USER();
        waitingGame = WAITING_GAME();
    }

    @Nested
    @DisplayName("팀 변경")
    class ChangeTeam {

        @Test
        void 팀_변경에_성공하면_이벤트를_발행한다() {
            // given
            GameParticipant participant = GUEST_PARTICIPANT(waitingGame, user);
            TeamChangeCommand command = new TeamChangeCommand(user.getId(), waitingGame.getId(), Team.POLICE);

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(participant);
            given(gameParticipantRepository.countByGameIdAndTeam(waitingGame.getId(), Team.POLICE)).willReturn(1);
            given(gameParticipantRepository.countByGameIdAndTeam(waitingGame.getId(), Team.ROBBER)).willReturn(1);
            given(lobbyEventFactory.createTeamUpdateEvent(any(), any(GameParticipant.class), anyInt(), anyInt())).willReturn(lobbyEvent);

            // when
            lobbyService.changeTeam(command);

            // then
            assertThat(participant.getTeam()).isEqualTo(Team.POLICE);
            then(eventPublisher).should().publishEvent(lobbyEvent);
        }

        @Test
        void 게임이_WAITING_상태가_아니면_예외가_발생한다() {
            // given
            Game inProgressGame = IN_PROGRESS_GAME();
            TeamChangeCommand command = new TeamChangeCommand(user.getId(), inProgressGame.getId(), Team.POLICE);

            given(gameRepository.getByGameId(inProgressGame.getId())).willReturn(inProgressGame);

            // when & then
            assertThatThrownBy(() -> lobbyService.changeTeam(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.GAME_ALREADY_STARTED.getDetail());
        }

        @Test
        void 참가자가_WAITING_상태가_아니면_예외가_발생한다() {
            // given
            GameParticipant aliveParticipant = ALIVE_ROBBER(waitingGame, user);
            TeamChangeCommand command = new TeamChangeCommand(user.getId(), waitingGame.getId(), Team.POLICE);

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(aliveParticipant);

            // when & then
            assertThatThrownBy(() -> lobbyService.changeTeam(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.LOBBY_ACTION_NOT_ALLOWED.getDetail());
        }
    }

    @Nested
    @DisplayName("준비 상태 변경")
    class UpdateReady {

        @Test
        void 준비_상태_변경에_성공하면_이벤트를_발행한다() {
            // given
            GameParticipant participant = GUEST_PARTICIPANT(waitingGame, user);
            ReadyUpdateCommand command = new ReadyUpdateCommand(user.getId(), waitingGame.getId(), true);

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(participant);
            given(lobbyEventFactory.createReadyUpdateEvent(any(), any())).willReturn(lobbyEvent);

            // when
            lobbyService.updateReady(command);

            // then
            assertThat(participant.isReady()).isTrue();
            then(eventPublisher).should().publishEvent(lobbyEvent);
        }

        @Test
        void 방장은_준비_상태를_해제할_수_없다() {
            // given
            GameParticipant hostParticipant = HOST_PARTICIPANT(waitingGame, user);
            ReadyUpdateCommand command = new ReadyUpdateCommand(user.getId(), waitingGame.getId(), false);

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(hostParticipant);

            // when & then
            assertThatThrownBy(() -> lobbyService.updateReady(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.HOST_CANNOT_UNREADY.getDetail());
        }

        @Test
        void 게임이_WAITING_상태가_아니면_예외가_발생한다() {
            // given
            Game inProgressGame = IN_PROGRESS_GAME();
            ReadyUpdateCommand command = new ReadyUpdateCommand(user.getId(), inProgressGame.getId(), true);

            given(gameRepository.getByGameId(inProgressGame.getId())).willReturn(inProgressGame);

            // when & then
            assertThatThrownBy(() -> lobbyService.updateReady(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.GAME_ALREADY_STARTED.getDetail());
        }
    }

    @Nested
    @DisplayName("게임 시작")
    class StartGame {

        @Test
        void 게임_시작에_성공하면_이벤트를_발행한다() {
            // given
            GameParticipant hostParticipant = HOST_PARTICIPANT(waitingGame, user);
            GameStartCommand command = new GameStartCommand(user.getId(), waitingGame.getId());

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(hostParticipant);
            given(gameParticipantRepository.countByGameIdAndTeam(waitingGame.getId(), Team.POLICE)).willReturn(1);
            given(gameParticipantRepository.countByGameIdAndTeam(waitingGame.getId(), Team.ROBBER)).willReturn(1);
            given(gameParticipantRepository.existsNotReadyParticipantByGameId(waitingGame.getId())).willReturn(false);
            given(lobbyEventFactory.createGameStartEvent(any(), any())).willReturn(lobbyEvent);

            // when
            lobbyService.startGame(command);

            // then
            assertThat(waitingGame.isInProgress()).isTrue();
            then(eventPublisher).should().publishEvent(lobbyEvent);
        }

        @Test
        void 방장이_아니면_예외가_발생한다() {
            // given
            GameParticipant guestParticipant = GUEST_PARTICIPANT(waitingGame, user);
            GameStartCommand command = new GameStartCommand(user.getId(), waitingGame.getId());

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(guestParticipant);

            // when & then
            assertThatThrownBy(() -> lobbyService.startGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.NOT_HOST.getDetail());
        }

        @Test
        void 경찰이_없으면_예외가_발생한다() {
            // given
            GameParticipant hostParticipant = HOST_PARTICIPANT(waitingGame, user);
            GameStartCommand command = new GameStartCommand(user.getId(), waitingGame.getId());

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(hostParticipant);
            given(gameParticipantRepository.countByGameIdAndTeam(waitingGame.getId(), Team.POLICE)).willReturn(0);
            given(gameParticipantRepository.countByGameIdAndTeam(waitingGame.getId(), Team.ROBBER)).willReturn(1);

            // when & then
            assertThatThrownBy(() -> lobbyService.startGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.INVALID_TEAM_COMPOSITION.getDetail());
        }

        @Test
        void 도둑이_없으면_예외가_발생한다() {
            // given
            GameParticipant hostParticipant = HOST_PARTICIPANT(waitingGame, user);
            GameStartCommand command = new GameStartCommand(user.getId(), waitingGame.getId());

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(hostParticipant);
            given(gameParticipantRepository.countByGameIdAndTeam(waitingGame.getId(), Team.POLICE)).willReturn(1);
            given(gameParticipantRepository.countByGameIdAndTeam(waitingGame.getId(), Team.ROBBER)).willReturn(0);

            // when & then
            assertThatThrownBy(() -> lobbyService.startGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.INVALID_TEAM_COMPOSITION.getDetail());
        }

        @Test
        void 준비하지_않은_참가자가_있으면_예외가_발생한다() {
            // given
            GameParticipant hostParticipant = HOST_PARTICIPANT(waitingGame, user);
            GameStartCommand command = new GameStartCommand(user.getId(), waitingGame.getId());

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(hostParticipant);
            given(gameParticipantRepository.countByGameIdAndTeam(waitingGame.getId(), Team.POLICE)).willReturn(1);
            given(gameParticipantRepository.countByGameIdAndTeam(waitingGame.getId(), Team.ROBBER)).willReturn(1);
            given(gameParticipantRepository.existsNotReadyParticipantByGameId(waitingGame.getId())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> lobbyService.startGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.NOT_ALL_READY.getDetail());
        }

        @Test
        void 게임이_WAITING_상태가_아니면_예외가_발생한다() {
            // given
            Game inProgressGame = IN_PROGRESS_GAME();
            GameStartCommand command = new GameStartCommand(user.getId(), inProgressGame.getId());

            given(gameRepository.getByGameId(inProgressGame.getId())).willReturn(inProgressGame);

            // when & then
            assertThatThrownBy(() -> lobbyService.startGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.GAME_ALREADY_STARTED.getDetail());
        }
    }

    @Nested
    @DisplayName("강제 퇴장")
    class KickMember {

        @Test
        void 강제_퇴장에_성공하면_참가자가_삭제되고_이벤트를_발행한다() {
            // given
            User guestUser = USER("guest");
            setId(user, 1L);
            setId(guestUser, 2L);

            GameParticipant hostParticipant = HOST_PARTICIPANT(waitingGame, user);
            GameParticipant guestParticipant = GUEST_PARTICIPANT(waitingGame, guestUser);
            setId(hostParticipant, 1L);
            setId(guestParticipant, 2L);

            KickCommand command = new KickCommand(user.getId(), waitingGame.getId(), guestParticipant.getId());

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(hostParticipant);
            given(gameParticipantRepository.getByIdAndGameId(guestParticipant.getId(), waitingGame.getId())).willReturn(guestParticipant);
            given(lobbyEventFactory.createKickEvent(any(), any())).willReturn(lobbyEvent);

            // when
            lobbyService.kickMember(command);

            // then
            then(gameParticipantRepository).should().delete(guestParticipant);
            then(eventPublisher).should().publishEvent(lobbyEvent);
        }

        @Test
        void 게임이_WAITING_상태가_아니면_예외가_발생한다() {
            // given
            Game inProgressGame = IN_PROGRESS_GAME();
            KickCommand command = new KickCommand(user.getId(), inProgressGame.getId(), 2L);

            given(gameRepository.getByGameId(inProgressGame.getId())).willReturn(inProgressGame);

            // when & then
            assertThatThrownBy(() -> lobbyService.kickMember(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.GAME_ALREADY_STARTED.getDetail());
        }

        @Test
        void 방장이_아니면_예외가_발생한다() {
            // given
            User targetUser = USER("target");
            setId(user, 1L);
            setId(targetUser, 2L);

            GameParticipant guestParticipant = GUEST_PARTICIPANT(waitingGame, user);
            GameParticipant targetParticipant = GUEST_PARTICIPANT(waitingGame, targetUser);
            setId(guestParticipant, 1L);
            setId(targetParticipant, 2L);

            KickCommand command = new KickCommand(user.getId(), waitingGame.getId(), targetParticipant.getId());

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(guestParticipant);
            given(gameParticipantRepository.getByIdAndGameId(targetParticipant.getId(), waitingGame.getId())).willReturn(targetParticipant);

            // when & then
            assertThatThrownBy(() -> lobbyService.kickMember(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.NOT_HOST.getDetail());
        }

        @Test
        void 자기_자신을_강제_퇴장하려_하면_예외가_발생한다() {
            // given
            GameParticipant hostParticipant = HOST_PARTICIPANT(waitingGame, user);
            setId(hostParticipant, 1L);

            KickCommand command = new KickCommand(user.getId(), waitingGame.getId(), hostParticipant.getId());

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(waitingGame.getId(), user.getId())).willReturn(hostParticipant);
            given(gameParticipantRepository.getByIdAndGameId(hostParticipant.getId(), waitingGame.getId())).willReturn(hostParticipant);

            // when & then
            assertThatThrownBy(() -> lobbyService.kickMember(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.CANNOT_KICK_YOURSELF.getDetail());
        }
    }

    @Nested
    @DisplayName("로비 조회")
    class GetLobbyInfo {

        @Test
        void 로비_정보_조회에_성공한다() {
            // given
            User hostUser = USER("host");
            setId(hostUser, 1L);
            setId(user, 2L);

            GameParticipant hostParticipant = HOST_PARTICIPANT(waitingGame, hostUser);
            GameParticipant guestParticipant = GUEST_PARTICIPANT(waitingGame, user);
            LobbyInfoCommand command = LobbyInfoCommand.of(user.getId(), waitingGame.getId());

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.findAllByGameIdWithUser(waitingGame.getId()))
                    .willReturn(List.of(hostParticipant, guestParticipant));

            // when
            lobbyService.getLobbyInfo(command);

            // then
            then(gameRepository).should().getByGameId(waitingGame.getId());
            then(gameParticipantRepository).should().findAllByGameIdWithUser(waitingGame.getId());
        }

        @Test
        void 게임이_WAITING_상태가_아니면_예외가_발생한다() {
            // given
            Game inProgressGame = IN_PROGRESS_GAME();
            LobbyInfoCommand command = LobbyInfoCommand.of(user.getId(), inProgressGame.getId());

            given(gameRepository.getByGameId(inProgressGame.getId())).willReturn(inProgressGame);

            // when & then
            assertThatThrownBy(() -> lobbyService.getLobbyInfo(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.GAME_ALREADY_STARTED.getDetail());
        }

        @Test
        void 해당_게임의_참가자가_아니면_예외가_발생한다() {
            // given
            User anotherUser = USER("another");
            setId(anotherUser, 999L);

            GameParticipant hostParticipant = HOST_PARTICIPANT(waitingGame, anotherUser);
            LobbyInfoCommand command = LobbyInfoCommand.of(user.getId(), waitingGame.getId());

            given(gameRepository.getByGameId(waitingGame.getId())).willReturn(waitingGame);
            given(gameParticipantRepository.findAllByGameIdWithUser(waitingGame.getId()))
                    .willReturn(List.of(hostParticipant));

            // when & then
            assertThatThrownBy(() -> lobbyService.getLobbyInfo(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.PARTICIPANT_NOT_FOUND.getDetail());
        }
    }
}
