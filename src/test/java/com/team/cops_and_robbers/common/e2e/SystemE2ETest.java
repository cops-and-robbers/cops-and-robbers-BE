package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.TestEventResponse;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.play.location.application.RunnerLocationService;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.system.application.SystemEventFactory;
import com.team.cops_and_robbers.play.system.application.SystemPublisher;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.play.system.presentation.dto.request.ArrestRequest;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@DisplayName("시스템 이벤트 E2E")
class SystemE2ETest extends WebSocketE2ETest {

    private static final String SYSTEM_CHANNEL = "/subscribe/game/%d/system";

    @Autowired
    private SystemPublisher systemPublisher;

    @Autowired
    private SystemEventFactory systemEventFactory;

    @Autowired
    private RunnerLocationService runnerLocationService;

    private record SystemSetup(
            Game game,
            GameParticipant chaserParticipant,
            GameParticipant runnerParticipant,
            String chaserToken,
            String runnerToken,
            String systemChannel,
            StompTestClient chaserClient,
            StompTestClient runnerClient
    ) {}

    private SystemSetup givenSystemSetup() throws Exception {
        User chaserUser = givenUser("chaser");
        User runnerUser = givenUser("runner");

        Game game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));
        GameParticipant chaserParticipant = givenChaser(game, chaserUser);
        GameParticipant runnerParticipant = givenRunner(game, runnerUser);

        String chaserToken = givenAccessToken(chaserUser);
        String runnerToken = givenAccessToken(runnerUser);
        String systemChannel = SYSTEM_CHANNEL.formatted(game.getId());

        StompTestClient chaserClient = connect(chaserToken);
        StompTestClient runnerClient = connect(runnerToken);
        chaserClient.subscribe(systemChannel, TestEventResponse.class);
        runnerClient.subscribe(systemChannel, TestEventResponse.class);

        return new SystemSetup(game, chaserParticipant, runnerParticipant, chaserToken, runnerToken, systemChannel, chaserClient, runnerClient);
    }

    @Test
    void 경찰_대기_시간이_지나면_CHASER_MOVE_START_이벤트를_수신한다() throws Exception {
        // given
        SystemSetup setup = givenSystemSetup();

        // when
        SystemEvent event = systemEventFactory.createChaserMoveStartEvent(setup.game().getId());
        systemPublisher.publish(event);

        // then
        TestEventResponse chaserEvent = setup.chaserClient().waitForMessage(setup.systemChannel(), 5);
        TestEventResponse runnerEvent = setup.runnerClient().waitForMessage(setup.systemChannel(), 5);

        assertThat(chaserEvent.type()).isEqualTo("POLICE_MOVE_START");
        assertThat(runnerEvent.type()).isEqualTo("POLICE_MOVE_START");
    }

    @Test
    void 위치_공개_주기가_도달하면_경찰이_RUNNER_LOCATION_REVEAL_이벤트를_수신한다() throws Exception {
        // given
        SystemSetup setup = givenSystemSetup();

        runnerLocationService.updateLocation(
                new LocationUpdateCommand(setup.game().getId(), setup.runnerParticipant().getId(), 37.5, 127.0)
        );

        // when
        List<SystemEventData.RunnerLocation> locations = runnerLocationService.revealRunnerLocations(setup.game().getId());
        SystemEvent event = systemEventFactory.createRunnerLocationRevealEvent(setup.game().getId(), locations);
        systemPublisher.publish(event);

        // then
        TestEventResponse chaserEvent = setup.chaserClient().waitForMessage(setup.systemChannel(), 5);

        assertThat(chaserEvent.type()).isEqualTo("ROBBER_LOCATION_REVEAL");
    }

    @Test
    void 경찰이_도둑을_체포하면_모든_참가자가_ARREST_이벤트를_수신한다() throws Exception {
        // given: 도둑 2명 (체포 후에도 게임 종료 안 됨)
        SystemSetup setup = givenSystemSetup();
        givenRunner(setup.game(), givenUser("runner2"));

        // when
        authenticated(setup.chaserToken())
                .body(new ArrestRequest(setup.runnerParticipant().getId()))
                .post("/api/games/{gameId}/system/arrest", setup.game().getId())
                .then()
                .statusCode(200);

        // then
        TestEventResponse chaserEvent = setup.chaserClient().waitForMessage(setup.systemChannel(), 5);
        TestEventResponse runnerEvent = setup.runnerClient().waitForMessage(setup.systemChannel(), 5);

        assertThat(chaserEvent.type()).isEqualTo("ARREST");
        assertThat(runnerEvent.type()).isEqualTo("ARREST");
    }

    @Test
    void 도둑이_감옥에서_탈출하면_모든_참가자가_ESCAPE_이벤트를_수신한다() throws Exception {
        // given
        SystemSetup setup = givenSystemSetup();
        setup.runnerParticipant().updateStatus(ParticipantStatus.JAILED);
        gameParticipantRepository.save(setup.runnerParticipant());

        // when
        authenticated(setup.runnerToken())
                .post("/api/games/{gameId}/system/escape", setup.game().getId())
                .then()
                .statusCode(204);

        // then
        TestEventResponse chaserEvent = setup.chaserClient().waitForMessage(setup.systemChannel(), 5);
        TestEventResponse runnerEvent = setup.runnerClient().waitForMessage(setup.systemChannel(), 5);

        assertThat(chaserEvent.type()).isEqualTo("ESCAPE");
        assertThat(runnerEvent.type()).isEqualTo("ESCAPE");
    }

    @Test
    void 모든_도둑이_체포되면_GAME_OVER_이벤트를_수신한다() throws Exception {
        // given: 마지막 도둑 한 명 남음
        SystemSetup setup = givenSystemSetup();

        // when: 마지막 도둑 체포 → ARREST + GAME_OVER 이벤트 발행
        authenticated(setup.chaserToken())
                .body(new ArrestRequest(setup.runnerParticipant().getId()))
                .post("/api/games/{gameId}/system/arrest", setup.game().getId())
                .then()
                .statusCode(200);

        // then: ARREST 이벤트 먼저 수신 후 GAME_OVER 수신
        TestEventResponse arrestEvent = setup.chaserClient().waitForMessage(setup.systemChannel(), 5);
        TestEventResponse gameOverEvent = setup.chaserClient().waitForMessage(setup.systemChannel(), 5);

        assertThat(arrestEvent.type()).isEqualTo("ARREST");
        assertThat(gameOverEvent.type()).isEqualTo("GAME_OVER");
    }

    @Nested
    @DisplayName("폴리곤 영역 시스템 이벤트 E2E")
    class WithPolygonArea {

        private SystemSetup givenSystemSetupPolygon() throws Exception {
            User chaserUser = givenUser("polyChaser");
            User runnerUser = givenUser("polyRunner");

            Game game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
            gameAreaRepository.save(GameAreaFixture.POLYGON_GAME_AREA(game));
            GameParticipant chaserParticipant = givenChaser(game, chaserUser);
            GameParticipant runnerParticipant = givenRunner(game, runnerUser);

            String chaserToken = givenAccessToken(chaserUser);
            String runnerToken = givenAccessToken(runnerUser);
            String systemChannel = SYSTEM_CHANNEL.formatted(game.getId());

            StompTestClient chaserClient = connect(chaserToken);
            StompTestClient runnerClient = connect(runnerToken);
            chaserClient.subscribe(systemChannel, TestEventResponse.class);
            runnerClient.subscribe(systemChannel, TestEventResponse.class);

            return new SystemSetup(game, chaserParticipant, runnerParticipant, chaserToken, runnerToken, systemChannel, chaserClient, runnerClient);
        }

        @Test
        void 폴리곤_영역에서_경찰이_도둑을_체포하면_ARREST_이벤트를_수신한다() throws Exception {
            // given: 도둑 2명 (체포 후에도 게임 종료 안 됨)
            SystemSetup setup = givenSystemSetupPolygon();
            givenRunner(setup.game(), givenUser("polyRunner2"));

            // when
            authenticated(setup.chaserToken())
                    .body(new ArrestRequest(setup.runnerParticipant().getId()))
                    .post("/api/games/{gameId}/system/arrest", setup.game().getId())
                    .then()
                    .statusCode(200);

            // then
            TestEventResponse chaserEvent = setup.chaserClient().waitForMessage(setup.systemChannel(), 5);
            assertThat(chaserEvent.type()).isEqualTo("ARREST");
        }

        @Test
        void 폴리곤_영역에서_모든_도둑이_체포되면_GAME_OVER_이벤트를_수신한다() throws Exception {
            // given: 마지막 도둑 한 명 남음
            SystemSetup setup = givenSystemSetupPolygon();

            // when
            authenticated(setup.chaserToken())
                    .body(new ArrestRequest(setup.runnerParticipant().getId()))
                    .post("/api/games/{gameId}/system/arrest", setup.game().getId())
                    .then()
                    .statusCode(200);

            // then
            TestEventResponse arrestEvent = setup.chaserClient().waitForMessage(setup.systemChannel(), 5);
            TestEventResponse gameOverEvent = setup.chaserClient().waitForMessage(setup.systemChannel(), 5);

            assertThat(arrestEvent.type()).isEqualTo("ARREST");
            assertThat(gameOverEvent.type()).isEqualTo("GAME_OVER");
        }
    }
}
