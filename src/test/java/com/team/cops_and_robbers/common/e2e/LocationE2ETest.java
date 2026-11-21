package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.TestEventResponse;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.play.location.application.RunnerLocationService;
import com.team.cops_and_robbers.play.location.presentation.dto.LocationUpdateRequest;
import com.team.cops_and_robbers.play.system.application.SystemEventFactory;
import com.team.cops_and_robbers.play.system.application.SystemPublisher;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@DisplayName("위치 이벤트 E2E")
class LocationE2ETest extends WebSocketE2ETest {

    private static final String SYSTEM_CHANNEL = "/subscribe/game/%d/system";

    @Autowired
    private RunnerLocationService runnerLocationService;

    @Autowired
    private SystemPublisher systemPublisher;

    @Autowired
    private SystemEventFactory systemEventFactory;

    private record LocationSetup(
            Game game,
            GameParticipant runnerParticipant,
            String chaserToken,
            String runnerToken,
            String systemChannel,
            StompTestClient chaserClient,
            StompTestClient runnerClient
    ) {}

    private LocationSetup givenLocationSetup() throws Exception {
        User chaserUser = givenUser("chaser");
        User runnerUser = givenUser("runner");

        Game game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));
        givenChaser(game, chaserUser);
        GameParticipant runnerParticipant = givenRunner(game, runnerUser);

        String chaserToken = givenAccessToken(chaserUser);
        String runnerToken = givenAccessToken(runnerUser);
        String systemChannel = SYSTEM_CHANNEL.formatted(game.getId());

        StompTestClient chaserClient = connect(chaserToken);
        StompTestClient runnerClient = connect(runnerToken);
        chaserClient.subscribe(systemChannel, TestEventResponse.class);
        runnerClient.subscribe(systemChannel, TestEventResponse.class);

        return new LocationSetup(game, runnerParticipant, chaserToken, runnerToken, systemChannel, chaserClient, runnerClient);
    }

    @Test
    void 도둑이_위치를_전송하면_서버에_위치가_저장된다() throws Exception {
        // given
        LocationSetup setup = givenLocationSetup();

        // when
        setup.runnerClient().send("/publish/game/" + setup.game().getId() + "/location",
                new LocationUpdateRequest(37.5, 127.0));

        Thread.sleep(500);

        // then
        List<SystemEventData.RunnerLocation> locations = runnerLocationService.revealRunnerLocations(setup.game().getId());

        assertThat(locations.get(0).participantId()).isEqualTo(setup.runnerParticipant().getId());
        assertThat(locations.get(0).latitude()).isEqualTo(37.5);
        assertThat(locations.get(0).longitude()).isEqualTo(127.0);
    }

    @Nested
    @DisplayName("폴리곤 영역 위치 이벤트 E2E")
    class WithPolygonArea {

        private LocationSetup givenLocationSetupPolygon() throws Exception {
            User chaserUser = givenUser("polyChaser");
            User runnerUser = givenUser("polyRunner");

            Game game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
            gameAreaRepository.save(GameAreaFixture.POLYGON_GAME_AREA(game));
            givenChaser(game, chaserUser);
            GameParticipant runnerParticipant = givenRunner(game, runnerUser);

            String chaserToken = givenAccessToken(chaserUser);
            String runnerToken = givenAccessToken(runnerUser);
            String systemChannel = SYSTEM_CHANNEL.formatted(game.getId());

            StompTestClient chaserClient = connect(chaserToken);
            StompTestClient runnerClient = connect(runnerToken);
            chaserClient.subscribe(systemChannel, TestEventResponse.class);
            runnerClient.subscribe(systemChannel, TestEventResponse.class);

            return new LocationSetup(game, runnerParticipant, chaserToken, runnerToken, systemChannel, chaserClient, runnerClient);
        }

        @Test
        void 폴리곤_영역에서_도둑이_위치를_전송하면_서버에_위치가_저장된다() throws Exception {
            // given
            LocationSetup setup = givenLocationSetupPolygon();

            // when
            setup.runnerClient().send("/publish/game/" + setup.game().getId() + "/location",
                    new LocationUpdateRequest(37.5, 127.0));

            Thread.sleep(500);

            // then
            List<SystemEventData.RunnerLocation> locations = runnerLocationService.revealRunnerLocations(setup.game().getId());
            assertThat(locations.get(0).participantId()).isEqualTo(setup.runnerParticipant().getId());
        }
    }

    @Test
    void 위치_공개_시_도둑_위치가_시스템_채널을_통해_공개된다() throws Exception {
        // given
        LocationSetup setup = givenLocationSetup();

        setup.runnerClient().send("/publish/game/" + setup.game().getId() + "/location",
                new LocationUpdateRequest(37.5, 127.0));

        Thread.sleep(500);

        // when
        List<SystemEventData.RunnerLocation> locations = runnerLocationService.revealRunnerLocations(setup.game().getId());
        systemPublisher.publish(systemEventFactory.createRunnerLocationRevealEvent(setup.game().getId(), locations));

        // then
        TestEventResponse chaserEvent = setup.chaserClient().waitForMessage(setup.systemChannel(), 5);

        assertThat(chaserEvent.type()).isEqualTo("ROBBER_LOCATION_REVEAL");
        assertThat(chaserEvent.data().get("locations").size()).isGreaterThan(0);
        assertThat(chaserEvent.data().get("locations").get(0).get("participantId").asLong())
                .isEqualTo(setup.runnerParticipant().getId());
    }
}
