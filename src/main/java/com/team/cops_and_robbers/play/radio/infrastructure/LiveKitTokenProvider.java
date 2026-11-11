package com.team.cops_and_robbers.play.radio.infrastructure;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Component
public class LiveKitTokenProvider implements RadioTokenProvider {

    private static final String GRANT_CLAIM = "video";
    private static final String NAME_CLAIM = "name";

    private final LiveKitProperties liveKitProperties;
    private final Clock clock;
    private final Key signingKey;

    public LiveKitTokenProvider(LiveKitProperties liveKitProperties, Clock clock) {
        this.liveKitProperties = liveKitProperties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(
                liveKitProperties.apiSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public RadioToken createJoinToken(String room, Long participantId, String nickname) {
        Instant now = clock.instant();
        Instant expiration = now.plus(liveKitProperties.tokenTtl());

        Map<String, Object> grant = Map.of(
                "roomJoin", true,
                "room", room,
                "canPublish", true,
                "canSubscribe", true,
                "canPublishData", false
        );

        String token = Jwts.builder()
                .setIssuer(liveKitProperties.apiKey())
                .setSubject(participantId.toString())
                .claim(NAME_CLAIM, nickname)
                .claim(GRANT_CLAIM, grant)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        return new RadioToken(token, LocalDateTime.ofInstant(expiration, clock.getZone()));
    }

    @Override
    public Duration tokenTtl() {
        return liveKitProperties.tokenTtl();
    }

    @Override
    public String serverUrl() {
        return liveKitProperties.url();
    }
}
