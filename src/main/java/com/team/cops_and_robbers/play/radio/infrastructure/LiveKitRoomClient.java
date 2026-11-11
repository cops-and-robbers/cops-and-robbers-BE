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
    private static final String WEBSOCKET_SCHEME_PATTERN = "^ws";
    private static final String HTTP_SCHEME = "http";

    private final RestClient restClient;
    private final LiveKitProperties liveKitProperties;
    private final LiveKitTokenProvider liveKitTokenProvider;

    public LiveKitRoomClient(
            LiveKitProperties liveKitProperties,
            LiveKitTokenProvider liveKitTokenProvider,
            RestClient.Builder builder
    ) {
        this.liveKitProperties = liveKitProperties;
        this.liveKitTokenProvider = liveKitTokenProvider;
        this.restClient = builder
                .baseUrl(liveKitProperties.url().replaceFirst(WEBSOCKET_SCHEME_PATTERN, HTTP_SCHEME))
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
                    .body(Map.of("room", liveKitProperties.prefixedRoom(room)))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Radio] 무전 방 닫기 완료 | room={}", room);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("[Radio] 닫을 무전 방 없음 | room={}", room);
        } catch (Exception e) {
            log.error("[Radio] 무전 방 닫기 실패 | room={}", room, e);
        }
    }
}
