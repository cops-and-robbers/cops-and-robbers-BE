package com.team.cops_and_robbers.play.radio.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("LiveKit 무전 방 클라이언트")
class LiveKitRoomClientTest {

    private static final String DELETE_ROOM_URL = "https://test.livekit.cloud/twirp/livekit.RoomService/DeleteRoom";
    private static final String ROOM = "game-1-police";

    private MockRestServiceServer server;
    private LiveKitRoomClient client;

    @BeforeEach
    void setUp() {
        LiveKitProperties properties = new LiveKitProperties(
                "wss://test.livekit.cloud", "test-livekit-key",
                "test-livekit-secret-0123456789abcdef", Duration.ofMinutes(2));
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new LiveKitRoomClient(properties, new LiveKitTokenProvider(properties, Clock.systemUTC()), builder);
    }

    @Test
    void 웹소켓_주소를_HTTPS로_바꿔_방_삭제를_요청한다() {
        server.expect(requestTo(DELETE_ROOM_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andExpect(content().json("{\"room\":\"game-1-police\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.closeRoom(ROOM);

        server.verify();
    }

    @Test
    void 방이_없으면_예외를_던지지_않는다() {
        server.expect(requestTo(DELETE_ROOM_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"not_found\",\"msg\":\"requested room does not exist\"}"));

        assertThatCode(() -> client.closeRoom(ROOM)).doesNotThrowAnyException();
    }

    @Test
    void 무전_서버가_실패해도_예외를_던지지_않는다() {
        server.expect(requestTo(DELETE_ROOM_URL)).andRespond(withServerError());

        assertThatCode(() -> client.closeRoom(ROOM)).doesNotThrowAnyException();
    }
}
