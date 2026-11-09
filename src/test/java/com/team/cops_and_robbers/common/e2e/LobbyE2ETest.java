package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.TestEventResponse;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.game.game.presentation.dto.request.CoordinatesRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameAreaRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameModeUpdateRequest;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.presentation.dto.request.GameJoinRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.request.ReadyUpdateRequest;
import com.team.cops_and_robbers.play.lobby.presentation.dto.request.TeamChangeRequest;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@DisplayName("로비 이벤트 E2E")
class LobbyE2ETest extends WebSocketE2ETest {

    private static final String LOBBY_CHANNEL = "/subscribe/game/%d/lobby";

    private record LobbySetup(
            Game game,
            String hostToken,
            String guestToken,
            String lobbyChannel,
            StompTestClient hostClient,
            StompTestClient guestClient
    ) {}

    private LobbySetup givenLobbySetup() throws Exception {
        User hostUser = givenUser("host");
        User guestUser = givenUser("guest");

        Game game = gameRepository.save(GameFixture.WAITING_GAME());
        gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));
        gameParticipantRepository.save(GameParticipantFixture.HOST_PARTICIPANT(game, hostUser));
        gameParticipantRepository.save(GameParticipantFixture.GUEST_PARTICIPANT(game, guestUser));

        return connectAndSubscribeLobby(game, hostUser, guestUser);
    }

    private LobbySetup connectAndSubscribeLobby(Game game, User hostUser, User guestUser) throws Exception {
        String hostToken = givenAccessToken(hostUser);
        String guestToken = givenAccessToken(guestUser);
        String lobbyChannel = LOBBY_CHANNEL.formatted(game.getId());

        StompTestClient hostClient = connect(hostToken);
        StompTestClient guestClient = connect(guestToken);
        hostClient.subscribe(lobbyChannel, TestEventResponse.class);
        guestClient.subscribe(lobbyChannel, TestEventResponse.class);

        return new LobbySetup(game, hostToken, guestToken, lobbyChannel, hostClient, guestClient);
    }

    @Test
    void 유저가_로비에_입장하면_ENTER_이벤트를_수신한다() throws Exception {
        // given
        LobbySetup setup = givenLobbySetup();

        User joinUser = givenUser("joinGuest");
        String joinToken = givenAccessToken(joinUser);

        // when
        authenticated(joinToken)
                .body(new GameJoinRequest(setup.game().getInviteCode()))
                .post("/api/games/join")
                .then()
                .statusCode(200);

        // then
        TestEventResponse hostEvent = setup.hostClient().waitForMessage(setup.lobbyChannel(), 5);
        TestEventResponse guestEvent = setup.guestClient().waitForMessage(setup.lobbyChannel(), 5);

        assertThat(hostEvent.type()).isEqualTo("ENTER");
        assertThat(guestEvent.type()).isEqualTo("ENTER");
    }

    @Test
    void 유저가_로비에서_퇴장하면_EXIT_이벤트를_수신한다() throws Exception {
        // given
        LobbySetup setup = givenLobbySetup();

        // when
        authenticated(setup.guestToken())
                .delete("/api/games/{gameId}/leave", setup.game().getId())
                .then()
                .statusCode(200);

        // then
        TestEventResponse hostEvent = setup.hostClient().waitForMessage(setup.lobbyChannel(), 5);

        assertThat(hostEvent.type()).isEqualTo("EXIT");
    }

    @Test
    void 팀을_변경하면_TEAM_UPDATE_이벤트를_수신한다() throws Exception {
        // given
        LobbySetup setup = givenLobbySetup();

        // when: ROBBER -> POLICE
        authenticated(setup.guestToken())
                .body(new TeamChangeRequest(Team.POLICE))
                .patch("/api/games/{gameId}/lobby/team", setup.game().getId())
                .then()
                .statusCode(204);

        // then
        TestEventResponse hostEvent = setup.hostClient().waitForMessage(setup.lobbyChannel(), 5);

        assertThat(hostEvent.type()).isEqualTo("TEAM_UPDATE");
    }

    @Test
    void 방장이_퇴장하면_HOST_CHANGED_이벤트를_수신한다() throws Exception {
        // given
        LobbySetup setup = givenLobbySetup();

        // when
        authenticated(setup.hostToken())
                .delete("/api/games/{gameId}/leave", setup.game().getId())
                .then()
                .statusCode(200);

        // then: 방장 퇴장 시 HOST_CHANGED + EXIT 두 이벤트가 발행됨
        TestEventResponse event1 = setup.guestClient().waitForMessage(setup.lobbyChannel(), 5);
        TestEventResponse event2 = setup.guestClient().waitForMessage(setup.lobbyChannel(), 5);

        assertThat(List.of(event1.type(), event2.type())).contains("HOST_CHANGED");
    }

    @Test
    void 게임_모드를_바꾸면_MODE_UPDATED_이벤트로_모드와_구역을_수신한다() throws Exception {
        // given
        LobbySetup setup = givenLobbySetup();
        GameAreaRequest area = new GameAreaRequest(
                AreaType.CIRCLE,
                new GameAreaRequest.CircleAreaRequest(new CoordinatesRequest(37.4979, 127.0276), 500, null, null),
                null
        );

        // when
        authenticated(setup.hostToken())
                .body(new GameModeUpdateRequest(GameMode.TAG, area))
                .put("/api/games/{gameId}/mode", setup.game().getId())
                .then()
                .statusCode(200);

        // then
        TestEventResponse guestEvent = setup.guestClient().waitForMessage(setup.lobbyChannel(), 5);

        assertThat(guestEvent.type()).isEqualTo("MODE_UPDATED");
        assertThat(guestEvent.data().get("gameMode").asText()).isEqualTo("TAG");
        assertThat(guestEvent.data().get("areaData").get("playgroundRadiusInMeters").asInt()).isEqualTo(500);
        assertThat(guestEvent.data().get("areaData").get("jailLatitude").isNull()).isTrue();
    }

    @Nested
    @DisplayName("구버전 앱 호환 — 2단계에서 삭제 (#226)")
    class LegacyReadyCompat {

        @Test
        void 유저가_로비에_입장하면_ENTER_이벤트의_isReady는_true다() throws Exception {
            // given
            LobbySetup setup = givenLobbySetup();

            User joinUser = givenUser("joinGuest");
            String joinToken = givenAccessToken(joinUser);

            // when
            authenticated(joinToken)
                    .body(new GameJoinRequest(setup.game().getInviteCode()))
                    .post("/api/games/join")
                    .then()
                    .statusCode(200);

            // then
            TestEventResponse hostEvent = setup.hostClient().waitForMessage(setup.lobbyChannel(), 5);

            assertThat(hostEvent.type()).isEqualTo("ENTER");
            assertThat(hostEvent.data().get("newParticipant").get("isReady").asBoolean()).isTrue();
        }

        @Test
        void 팀을_변경하면_TEAM_UPDATE_이벤트의_isReady는_true다() throws Exception {
            // given
            LobbySetup setup = givenLobbySetup();

            // when: ROBBER -> POLICE
            authenticated(setup.guestToken())
                    .body(new TeamChangeRequest(Team.POLICE))
                    .patch("/api/games/{gameId}/lobby/team", setup.game().getId())
                    .then()
                    .statusCode(204);

            // then
            TestEventResponse hostEvent = setup.hostClient().waitForMessage(setup.lobbyChannel(), 5);

            assertThat(hostEvent.type()).isEqualTo("TEAM_UPDATE");
            assertThat(hostEvent.data().get("participant").get("isReady").asBoolean()).isTrue();
        }

        @Test
        void 방장이_퇴장하면_HOST_CHANGED_이벤트의_isReady는_true다() throws Exception {
            // given
            LobbySetup setup = givenLobbySetup();

            // when
            authenticated(setup.hostToken())
                    .delete("/api/games/{gameId}/leave", setup.game().getId())
                    .then()
                    .statusCode(200);

            // then: 방장 퇴장 시 HOST_CHANGED + EXIT 두 이벤트가 발행됨
            TestEventResponse event1 = setup.guestClient().waitForMessage(setup.lobbyChannel(), 5);
            TestEventResponse event2 = setup.guestClient().waitForMessage(setup.lobbyChannel(), 5);

            TestEventResponse hostChanged = List.of(event1, event2).stream()
                    .filter(event -> event.type().equals("HOST_CHANGED"))
                    .findFirst()
                    .orElseThrow();
            assertThat(hostChanged.data().get("newHost").get("isReady").asBoolean()).isTrue();
        }

        @Test
        void 준비_상태를_변경해도_이벤트를_발행하지_않는다() throws Exception {
            // given
            LobbySetup setup = givenLobbySetup();

            // when
            authenticated(setup.guestToken())
                    .body(new ReadyUpdateRequest(true))
                    .patch("/api/games/{gameId}/lobby/ready", setup.game().getId())
                    .then()
                    .statusCode(204);

            // then
            assertThat(setup.hostClient().<TestEventResponse>waitForMessage(setup.lobbyChannel(), 1)).isNull();
        }
    }

    @Nested
    @DisplayName("폴리곤 영역 로비 이벤트 E2E")
    class WithPolygonArea {

        private LobbySetup givenLobbySetupPolygon() throws Exception {
            User hostUser = givenUser("polyHost");
            User guestUser = givenUser("polyGuest");

            Game game = gameRepository.save(GameFixture.WAITING_GAME());
            gameAreaRepository.save(GameAreaFixture.POLYGON_GAME_AREA(game));
            gameParticipantRepository.save(GameParticipantFixture.HOST_PARTICIPANT(game, hostUser));
            gameParticipantRepository.save(GameParticipantFixture.GUEST_PARTICIPANT(game, guestUser));

            return connectAndSubscribeLobby(game, hostUser, guestUser);
        }

        @Test
        void 폴리곤_영역에서_유저가_퇴장하면_EXIT_이벤트를_수신한다() throws Exception {
            // given
            LobbySetup setup = givenLobbySetupPolygon();

            // when
            authenticated(setup.guestToken())
                    .delete("/api/games/{gameId}/leave", setup.game().getId())
                    .then()
                    .statusCode(200);

            // then
            TestEventResponse hostEvent = setup.hostClient().waitForMessage(setup.lobbyChannel(), 5);
            assertThat(hostEvent.type()).isEqualTo("EXIT");
        }

        @Test
        void 폴리곤_영역에서_방장이_퇴장하면_HOST_CHANGED_이벤트를_수신한다() throws Exception {
            // given
            LobbySetup setup = givenLobbySetupPolygon();

            // when
            authenticated(setup.hostToken())
                    .delete("/api/games/{gameId}/leave", setup.game().getId())
                    .then()
                    .statusCode(200);

            // then
            TestEventResponse event1 = setup.guestClient().waitForMessage(setup.lobbyChannel(), 5);
            TestEventResponse event2 = setup.guestClient().waitForMessage(setup.lobbyChannel(), 5);

            assertThat(List.of(event1.type(), event2.type())).contains("HOST_CHANGED");
        }
    }

    @Test
    void 게스트가_준비하지_않아도_방장이_게임을_시작하면_모든_참가자가_GAME_START_이벤트를_수신한다() throws Exception {
        // given
        LobbySetup setup = givenLobbySetup();

        // when
        authenticated(setup.hostToken())
                .post("/api/games/{gameId}/lobby/start", setup.game().getId())
                .then()
                .statusCode(204);

        // then
        TestEventResponse hostEvent = setup.hostClient().waitForMessage(setup.lobbyChannel(), 5);
        TestEventResponse guestEvent = setup.guestClient().waitForMessage(setup.lobbyChannel(), 5);

        assertThat(hostEvent.type()).isEqualTo("GAME_START");
        assertThat(guestEvent.type()).isEqualTo("GAME_START");
    }
}
