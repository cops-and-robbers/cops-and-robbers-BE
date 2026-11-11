package com.team.cops_and_robbers.play.radio.infrastructure;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "livekit")
public record LiveKitProperties(
        @NotBlank String url,
        @NotBlank String apiKey,
        @NotBlank String apiSecret,
        Duration tokenTtl,
        @NotBlank String roomPrefix
) {

    private static final String PREFIXED_ROOM_FORMAT = "%s-%s";

    public String prefixedRoom(String room) {
        return String.format(PREFIXED_ROOM_FORMAT, roomPrefix, room);
    }
}
