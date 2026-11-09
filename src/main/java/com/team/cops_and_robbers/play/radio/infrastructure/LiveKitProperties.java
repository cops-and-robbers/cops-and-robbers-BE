package com.team.cops_and_robbers.play.radio.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "livekit")
public record LiveKitProperties(
        String url,
        String apiKey,
        String apiSecret,
        Duration tokenTtl
) {
}
