package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.TestEventResponse;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.location.presentation.dto.LocationUpdateRequest;
import com.team.cops_and_robbers.play.system.application.SystemEventFactory;
import com.team.cops_and_robbers.play.system.application.SystemPublisher;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
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
    private RobberLocationService robberLocationService;

    @Autowired
    private SystemPublisher systemPublisher;

    @Autowired
    private SystemEventFactory systemEventFactory;

    private record LocationSetup(
            Game game,
            GameParticipant robberParticipant,
            String policeToken,
            String robberToken,
            String systemChannel,
            StompTestClient policeClient,
            StompTestClient robberClient
    ) {}

    private LocationSetup givenLocationSetup() throws Exception {
        User policeUser = givenUser("police");
        User robberUser = givenUser("robber");

        Game game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        gameAreaRepository.save(GameAreaFixture.GAME_AREA(game));
        givenPolice(game, policeUser);
        GameParticipant robberParticipant = givenRobber(game, robberUser);

        String policeToken = givenAccessToken(policeUser);
        String robberToken = givenAccessToken(robberUser);
        String systemChannel = SYSTEM_CHANNEL.formatted(game.getId());

        StompTestClient policeClient = connect(policeToken);
        StompTestClient robberClient = connect(robberToken);
        policeClient.subscribe(systemChannel, TestEventResponse.class);
        robberClient.subscribe(systemChannel, TestEventResponse.class);

        return new LocationSetup(game, robberParticipant, policeToken, robberToken, systemChannel, policeClient, robberClient);
    }

    @Test
    void 도둑이_위치를_전송하면_서버에_위치가_저장된다() throws Exception {
        // given
        LocationSetup setup = givenLocationSetup();

        // when
        setup.robberClient().send("/publish/game/" + setup.game().getId() + "/location",
                new LocationUpdateRequest(37.5, 127.0));

        Thread.sleep(500);

        // then
        List<SystemEventData.RobberLocation> locations = robberLocationService.getCurrentRobberLocations(setup.game().getId());

        assertThat(locations.get(0).participantId()).isEqualTo(setup.robberParticipant().getId());
        assertThat(locations.get(0).latitude()).isEqualTo(37.5);
        assertThat(locations.get(0).longitude()).isEqualTo(127.0);
    }

    @Test
    void 위치_공개_시_도둑_위치가_시스템_채널을_통해_공개된다() throws Exception {
        // given
        LocationSetup setup = givenLocationSetup();

        setup.robberClient().send("/publish/game/" + setup.game().getId() + "/location",
                new LocationUpdateRequest(37.5, 127.0));

        Thread.sleep(500);

        // when
        List<SystemEventData.RobberLocation> locations = robberLocationService.getCurrentRobberLocations(setup.game().getId());
        systemPublisher.publish(systemEventFactory.createRobberLocationRevealEvent(setup.game().getId(), locations));

        // then
        TestEventResponse policeEvent = setup.policeClient().waitForMessage(setup.systemChannel(), 5);

        assertThat(policeEvent.type()).isEqualTo("ROBBER_LOCATION_REVEAL");
        assertThat(policeEvent.data().get("locations").size()).isGreaterThan(0);
        assertThat(policeEvent.data().get("locations").get(0).get("participantId").asLong())
                .isEqualTo(setup.robberParticipant().getId());
    }
}
