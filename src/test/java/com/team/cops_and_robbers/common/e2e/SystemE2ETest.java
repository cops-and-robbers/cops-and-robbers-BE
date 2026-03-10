package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.TestEventResponse;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.system.application.SystemEventFactory;
import com.team.cops_and_robbers.play.system.application.SystemPublisher;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.play.system.presentation.dto.request.ArrestRequest;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
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
    private RobberLocationService robberLocationService;

    @Test
    void 경찰_대기_시간이_지나면_POLICE_MOVE_START_이벤트를_수신한다() throws Exception {
        // given
        User policeUser = givenUser("police");
        User robberUser = givenUser("robber");

        Game game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        gameAreaRepository.save(GameAreaFixture.GAME_AREA(game));
        givenPolice(game, policeUser);
        givenRobber(game, robberUser);

        String policeToken = givenAccessToken(policeUser);
        String robberToken = givenAccessToken(robberUser);
        String systemChannel = SYSTEM_CHANNEL.formatted(game.getId());

        StompTestClient policeClient = connect(policeToken);
        StompTestClient robberClient = connect(robberToken);
        policeClient.subscribe(systemChannel, TestEventResponse.class);
        robberClient.subscribe(systemChannel, TestEventResponse.class);

        // when
        SystemEvent event = systemEventFactory.createPoliceMoveStartEvent(game.getId());
        systemPublisher.publish(event);

        // then
        TestEventResponse policeEvent = policeClient.waitForMessage(systemChannel, 5);
        TestEventResponse robberEvent = robberClient.waitForMessage(systemChannel, 5);

        assertThat(policeEvent.type()).isEqualTo("POLICE_MOVE_START");
        assertThat(robberEvent.type()).isEqualTo("POLICE_MOVE_START");
    }

    @Test
    void 위치_공개_주기가_도달하면_경찰이_ROBBER_LOCATION_REVEAL_이벤트를_수신한다() throws Exception {
        // given
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

        robberLocationService.updateLocation(
                new LocationUpdateCommand(game.getId(), robberParticipant.getId(), 37.5, 127.0)
        );

        // when
        List<SystemEventData.RobberLocation> locations = robberLocationService.getCurrentRobberLocations(game.getId());
        SystemEvent event = systemEventFactory.createRobberLocationRevealEvent(game.getId(), locations);
        systemPublisher.publish(event);

        // then
        TestEventResponse policeEvent = policeClient.waitForMessage(systemChannel, 5);

        assertThat(policeEvent.type()).isEqualTo("ROBBER_LOCATION_REVEAL");
    }

    @Test
    void 경찰이_도둑을_체포하면_모든_참가자가_ARREST_이벤트를_수신한다() throws Exception {
        // given: 도둑 2명 (체포 후에도 게임 종료 안 됨)
        User policeUser = givenUser("police");
        User robberUser = givenUser("robber");
        User robber2User = givenUser("robber2");

        Game game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        gameAreaRepository.save(GameAreaFixture.GAME_AREA(game));
        givenPolice(game, policeUser);
        GameParticipant robberParticipant = givenRobber(game, robberUser);
        givenRobber(game, robber2User);

        String policeToken = givenAccessToken(policeUser);
        String robberToken = givenAccessToken(robberUser);
        String systemChannel = SYSTEM_CHANNEL.formatted(game.getId());

        StompTestClient policeClient = connect(policeToken);
        StompTestClient robberClient = connect(robberToken);
        policeClient.subscribe(systemChannel, TestEventResponse.class);
        robberClient.subscribe(systemChannel, TestEventResponse.class);

        // when
        authenticated(policeToken)
                .body(new ArrestRequest(robberParticipant.getId()))
                .post("/api/games/{gameId}/system/arrest", game.getId())
                .then()
                .statusCode(200);

        // then
        TestEventResponse policeEvent = policeClient.waitForMessage(systemChannel, 5);
        TestEventResponse robberEvent = robberClient.waitForMessage(systemChannel, 5);

        assertThat(policeEvent.type()).isEqualTo("ARREST");
        assertThat(robberEvent.type()).isEqualTo("ARREST");
    }

    @Test
    void 도둑이_감옥에서_탈출하면_모든_참가자가_ESCAPE_이벤트를_수신한다() throws Exception {
        // given
        User policeUser = givenUser("police");
        User robberUser = givenUser("robber");

        Game game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        gameAreaRepository.save(GameAreaFixture.GAME_AREA(game));
        givenPolice(game, policeUser);
        gameParticipantRepository.save(GameParticipantFixture.JAILED_ROBBER(game, robberUser));

        String policeToken = givenAccessToken(policeUser);
        String robberToken = givenAccessToken(robberUser);
        String systemChannel = SYSTEM_CHANNEL.formatted(game.getId());

        StompTestClient policeClient = connect(policeToken);
        StompTestClient robberClient = connect(robberToken);
        policeClient.subscribe(systemChannel, TestEventResponse.class);
        robberClient.subscribe(systemChannel, TestEventResponse.class);

        // when
        authenticated(robberToken)
                .post("/api/games/{gameId}/system/escape", game.getId())
                .then()
                .statusCode(204);

        // then
        TestEventResponse policeEvent = policeClient.waitForMessage(systemChannel, 5);
        TestEventResponse robberEvent = robberClient.waitForMessage(systemChannel, 5);

        assertThat(policeEvent.type()).isEqualTo("ESCAPE");
        assertThat(robberEvent.type()).isEqualTo("ESCAPE");
    }

    @Test
    void 모든_도둑이_체포되면_GAME_OVER_이벤트를_수신한다() throws Exception {
        // given: 마지막 도둑 한 명 남음
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

        // when: 마지막 도둑 체포 → ARREST + GAME_OVER 이벤트 발행
        authenticated(policeToken)
                .body(new ArrestRequest(robberParticipant.getId()))
                .post("/api/games/{gameId}/system/arrest", game.getId())
                .then()
                .statusCode(200);

        // then: ARREST 이벤트 먼저 수신 후 GAME_OVER 수신
        TestEventResponse arrestEvent = policeClient.waitForMessage(systemChannel, 5);
        TestEventResponse gameOverEvent = policeClient.waitForMessage(systemChannel, 5);

        assertThat(arrestEvent.type()).isEqualTo("ARREST");
        assertThat(gameOverEvent.type()).isEqualTo("GAME_OVER");
    }
}
