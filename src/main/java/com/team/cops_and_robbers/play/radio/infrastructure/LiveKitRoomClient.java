package com.team.cops_and_robbers.play.radio.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class LiveKitRoomClient implements RadioRoomClient {

    private static final String DELETE_ROOM_PATH = "/twirp/livekit.RoomService/DeleteRoom";
    private static final String WEBSOCKET_SCHEME = "^ws";
    private static final String HTTP_SCHEME = "http";

    private final RestClient restClient;
    private final LiveKitTokenProvider liveKitTokenProvider;

    public LiveKitRoomClient(
            LiveKitProperties liveKitProperties,
            LiveKitTokenProvider liveKitTokenProvider,
            RestClient.Builder builder
    ) {
        this.liveKitTokenProvider = liveKitTokenProvider;
        this.restClient = builder
                .baseUrl(liveKitProperties.url().replaceFirst(WEBSOCKET_SCHEME, HTTP_SCHEME))
                .build();
    }

    @Async
    @Override
    public void closeRoom(String room) {
        try {
            restClient.post()
                    .uri(DELETE_ROOM_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + liveKitTokenProvider.createRoomManageToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("room", room))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Radio] Room closed | room={}", room);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("[Radio] Room already closed | room={}", room);
        } catch (Exception e) {
            log.error("[Radio] Failed to close room | room={}", room, e);
        }
    }
}
