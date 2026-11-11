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
        Duration tokenTtl
) {
}
