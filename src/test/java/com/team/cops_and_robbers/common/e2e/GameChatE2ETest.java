package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.play.chat.domain.ChatScope;
import com.team.cops_and_robbers.play.chat.presentation.dto.ChatRequest;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("e2e")
@DisplayName("게임 채팅 E2E")
class GameChatE2ETest extends WebSocketE2ETest {

    private static final String CHAT_CHASER_CHANNEL = "/subscribe/game/%d/chat/chaser";
    private static final String LEGACY_CHAT_CHASER_CHANNEL = "/subscribe/game/%d/chat/police";
    private static final String CHAT_PUBLISH = "/publish/game/%d/chat";

    @Test
    @SuppressWarnings("unchecked")
    void 팀_채팅은_새_구독_경로와_옛_구독_경로에_모두_발행된다() throws Exception {
        // given
        User chaserUser = givenUser("chaser");
        Game game = gameRepository.save(GameFixture.IN_PROGRESS_GAME());
        gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));
        givenChaser(game, chaserUser);
        givenRunner(game, givenUser("runner"));

        String channel = CHAT_CHASER_CHANNEL.formatted(game.getId());
        String legacyChannel = LEGACY_CHAT_CHASER_CHANNEL.formatted(game.getId());
        StompTestClient chaserClient = connect(givenAccessToken(chaserUser));
        chaserClient.subscribe(channel, Map.class);
        chaserClient.subscribe(legacyChannel, Map.class);

        // when
        chaserClient.send(CHAT_PUBLISH.formatted(game.getId()), new ChatRequest("모여", ChatScope.TEAM));

        // then
        Map<String, Object> message = chaserClient.waitForMessage(channel, 5);
        Map<String, Object> legacyMessage = chaserClient.waitForMessage(legacyChannel, 5);
        assertSoftly(softly -> {
            softly.assertThat(message.get("message")).isEqualTo("모여");
            softly.assertThat(legacyMessage.get("message")).isEqualTo("모여");
            softly.assertThat(((Map<String, Object>) message.get("sender")).get("team")).isEqualTo("POLICE");
        });
    }
}
