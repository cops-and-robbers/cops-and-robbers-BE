package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.TestEventResponse;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.presentation.dto.request.GameJoinRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.request.ReadyUpdateRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.request.TeamChangeRequest;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@DisplayName("로비 이벤트 E2E")
class LobbyE2ETest extends WebSocketE2ETest {

    private static final String LOBBY_CHANNEL = "/subscribe/game/%d/lobby";

    @Test
    void 유저가_로비에_입장하면_ENTER_이벤트를_수신한다() throws Exception {
        // given
        User hostUser = givenUser("host");
        User guestUser = givenUser("guest");

        Game game = gameRepository.save(GameFixture.WAITING_GAME());
        gameAreaRepository.save(GameAreaFixture.GAME_AREA(game));
        gameParticipantRepository.save(GameParticipantFixture.HOST_PARTICIPANT(game, hostUser));
        gameParticipantRepository.save(GameParticipantFixture.GUEST_PARTICIPANT(game, guestUser));

        String hostToken = givenAccessToken(hostUser);
        String guestToken = givenAccessToken(guestUser);
        String lobbyChannel = LOBBY_CHANNEL.formatted(game.getId());

        StompTestClient hostClient = connect(hostToken);
        StompTestClient guestClient = connect(guestToken);
        hostClient.subscribe(lobbyChannel, TestEventResponse.class);
        guestClient.subscribe(lobbyChannel, TestEventResponse.class);

        User joinUser = givenUser("joinGuest");
        String joinToken = givenAccessToken(joinUser);

        // when
        authenticated(joinToken)
                .body(new GameJoinRequest(game.getInviteCode()))
                .post("/api/games/join")
                .then()
                .statusCode(200);
        // then
        TestEventResponse hostEvent = hostClient.waitForMessage(lobbyChannel, 5);
        TestEventResponse guestEvent = guestClient.waitForMessage(lobbyChannel, 5);

        assertThat(hostEvent.type()).isEqualTo("ENTER");
        assertThat(guestEvent.type()).isEqualTo("ENTER");
    }

    @Test
    void 유저가_로비에서_퇴장하면_EXIT_이벤트를_수신한다() throws Exception {
        // given
        User hostUser = givenUser("host");
        User guestUser = givenUser("guest");

        Game game = gameRepository.save(GameFixture.WAITING_GAME());
        gameAreaRepository.save(GameAreaFixture.GAME_AREA(game));
        gameParticipantRepository.save(GameParticipantFixture.HOST_PARTICIPANT(game, hostUser));
        gameParticipantRepository.save(GameParticipantFixture.GUEST_PARTICIPANT(game, guestUser));

        String hostToken = givenAccessToken(hostUser);
        String guestToken = givenAccessToken(guestUser);
        String lobbyChannel = LOBBY_CHANNEL.formatted(game.getId());

        StompTestClient hostClient = connect(hostToken);
        StompTestClient guestClient = connect(guestToken);
        hostClient.subscribe(lobbyChannel, TestEventResponse.class);
        guestClient.subscribe(lobbyChannel, TestEventResponse.class);

        // when
        authenticated(guestToken)
                .delete("/api/games/{gameId}/leave", game.getId())
                .then()
                .statusCode(200);

        // then
        TestEventResponse hostEvent = hostClient.waitForMessage(lobbyChannel, 5);

        assertThat(hostEvent.type()).isEqualTo("EXIT");
    }

    @Test
    void 팀을_변경하면_TEAM_UPDATE_이벤트를_수신한다() throws Exception {
        // given
        User hostUser = givenUser("host");
        User guestUser = givenUser("guest");

        Game game = gameRepository.save(GameFixture.WAITING_GAME());
        gameAreaRepository.save(GameAreaFixture.GAME_AREA(game));
        gameParticipantRepository.save(GameParticipantFixture.HOST_PARTICIPANT(game, hostUser));
        gameParticipantRepository.save(GameParticipantFixture.GUEST_PARTICIPANT(game, guestUser));

        String hostToken = givenAccessToken(hostUser);
        String guestToken = givenAccessToken(guestUser);
        String lobbyChannel = LOBBY_CHANNEL.formatted(game.getId());

        StompTestClient hostClient = connect(hostToken);
        StompTestClient guestClient = connect(guestToken);
        hostClient.subscribe(lobbyChannel, TestEventResponse.class);
        guestClient.subscribe(lobbyChannel, TestEventResponse.class);

        // when: ROBBER -> POLICE
        authenticated(guestToken)
                .body(new TeamChangeRequest(Team.POLICE))
                .patch("/api/games/{gameId}/lobby/team", game.getId())
                .then()
                .statusCode(204);

        // then
        TestEventResponse hostEvent = hostClient.waitForMessage(lobbyChannel, 5);

        assertThat(hostEvent.type()).isEqualTo("TEAM_UPDATE");
    }

    @Test
    void 준비_상태를_변경하면_READY_UPDATE_이벤트를_수신한다() throws Exception {
        // given
        User hostUser = givenUser("host");
        User guestUser = givenUser("guest");

        Game game = gameRepository.save(GameFixture.WAITING_GAME());
        gameAreaRepository.save(GameAreaFixture.GAME_AREA(game));
        gameParticipantRepository.save(GameParticipantFixture.HOST_PARTICIPANT(game, hostUser));
        gameParticipantRepository.save(GameParticipantFixture.GUEST_PARTICIPANT(game, guestUser));

        String hostToken = givenAccessToken(hostUser);
        String guestToken = givenAccessToken(guestUser);
        String lobbyChannel = LOBBY_CHANNEL.formatted(game.getId());

        StompTestClient hostClient = connect(hostToken);
        StompTestClient guestClient = connect(guestToken);
        hostClient.subscribe(lobbyChannel, TestEventResponse.class);
        guestClient.subscribe(lobbyChannel, TestEventResponse.class);

        // when
        authenticated(guestToken)
                .body(new ReadyUpdateRequest(true))
                .patch("/api/games/{gameId}/lobby/ready", game.getId())
                .then()
                .statusCode(204);

        // then
        TestEventResponse hostEvent = hostClient.waitForMessage(lobbyChannel, 5);

        assertThat(hostEvent.type()).isEqualTo("READY_UPDATE");
    }

    @Test
    void 방장이_퇴장하면_HOST_CHANGED_이벤트를_수신한다() throws Exception {
        // given
        User hostUser = givenUser("host");
        User guestUser = givenUser("guest");

        Game game = gameRepository.save(GameFixture.WAITING_GAME());
        gameAreaRepository.save(GameAreaFixture.GAME_AREA(game));
        gameParticipantRepository.save(GameParticipantFixture.HOST_PARTICIPANT(game, hostUser));
        gameParticipantRepository.save(GameParticipantFixture.GUEST_PARTICIPANT(game, guestUser));

        String hostToken = givenAccessToken(hostUser);
        String guestToken = givenAccessToken(guestUser);
        String lobbyChannel = LOBBY_CHANNEL.formatted(game.getId());

        StompTestClient hostClient = connect(hostToken);
        StompTestClient guestClient = connect(guestToken);
        hostClient.subscribe(lobbyChannel, TestEventResponse.class);
        guestClient.subscribe(lobbyChannel, TestEventResponse.class);

        // when
        authenticated(hostToken)
                .delete("/api/games/{gameId}/leave", game.getId())
                .then()
                .statusCode(200);

        // then
        TestEventResponse guestEvent = guestClient.waitForMessage(lobbyChannel, 5);

        assertThat(guestEvent.type()).isEqualTo("HOST_CHANGED");
    }

    @Test
    void 방장이_게임을_시작하면_모든_참가자가_GAME_START_이벤트를_수신한다() throws Exception {
        // given
        User hostUser = givenUser("host");
        User guestUser = givenUser("guest");

        Game game = gameRepository.save(GameFixture.WAITING_GAME());
        gameAreaRepository.save(GameAreaFixture.GAME_AREA(game));
        gameParticipantRepository.save(GameParticipantFixture.HOST_PARTICIPANT(game, hostUser));
        gameParticipantRepository.save(GameParticipantFixture.READY_PARTICIPANT(game, guestUser));

        String hostToken = givenAccessToken(hostUser);
        String guestToken = givenAccessToken(guestUser);
        String lobbyChannel = LOBBY_CHANNEL.formatted(game.getId());

        StompTestClient hostClient = connect(hostToken);
        StompTestClient guestClient = connect(guestToken);
        hostClient.subscribe(lobbyChannel, TestEventResponse.class);
        guestClient.subscribe(lobbyChannel, TestEventResponse.class);

        // when
        authenticated(hostToken)
                .post("/api/games/{gameId}/lobby/start", game.getId())
                .then()
                .statusCode(204);

        // then
        TestEventResponse hostEvent = hostClient.waitForMessage(lobbyChannel, 5);
        TestEventResponse guestEvent = guestClient.waitForMessage(lobbyChannel, 5);

        assertThat(hostEvent.type()).isEqualTo("GAME_START");
        assertThat(guestEvent.type()).isEqualTo("GAME_START");
    }
}
