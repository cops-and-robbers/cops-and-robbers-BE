package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@Tag("e2e")
@DisplayName("소켓 엔드포인트 E2E")
class SocketEndpointE2ETest extends WebSocketE2ETest {

    @Test
    void 신규_경로로_소켓에_연결할_수_있다() {
        User user = givenUser("connector");
        String token = givenAccessToken(user);

        assertThatCode(() -> connect(token))
                .doesNotThrowAnyException();
    }

    @Test
    void 구버전_앱_호환을_위해_레거시_경로로도_연결할_수_있다() {
        User user = givenUser("legacyConnector");
        String token = givenAccessToken(user);

        assertThatCode(() -> connect(token, StompTestClient.LEGACY_SOCKET_PATH))
                .doesNotThrowAnyException();
    }
}
