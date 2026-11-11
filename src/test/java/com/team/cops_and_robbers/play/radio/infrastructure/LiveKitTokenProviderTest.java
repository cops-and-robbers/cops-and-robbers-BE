package com.team.cops_and_robbers.play.radio.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class LiveKitTokenProviderTest {

    private static final String API_KEY = "test-livekit-key";
    private static final String API_SECRET = "test-livekit-secret-0123456789abcdef";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(2);
    private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");

    private final LiveKitTokenProvider liveKitTokenProvider = new LiveKitTokenProvider(
            new LiveKitProperties("wss://test.livekit.cloud", API_KEY, API_SECRET, TOKEN_TTL),
            Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))
    );

    @Test
    void 원문_secret으로_서명하고_LiveKit_입장_규격의_claim을_담는다() {
        // when
        RadioToken token = liveKitTokenProvider.createJoinToken("game-1-police", 101L, "경찰1");

        // then
        Jws<Claims> jws = parse(token.token());
        Claims claims = jws.getBody();
        Map<?, ?> grant = claims.get("video", Map.class);

        assertSoftly(softly -> {
            softly.assertThat(jws.getHeader().getAlgorithm()).isEqualTo("HS256");
            softly.assertThat(claims.getIssuer()).isEqualTo(API_KEY);
            softly.assertThat(claims.getSubject()).isEqualTo("101");
            softly.assertThat(claims.get("name", String.class)).isEqualTo("경찰1");
            softly.assertThat(grant.get("roomJoin")).isEqualTo(true);
            softly.assertThat(grant.get("room")).isEqualTo("game-1-police");
            softly.assertThat(grant.get("canPublish")).isEqualTo(true);
            softly.assertThat(grant.get("canSubscribe")).isEqualTo(true);
            softly.assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                    .isEqualTo(TOKEN_TTL.toMillis());
            softly.assertThat(token.expiresAt()).isEqualTo(LocalDateTime.of(2026, 11, 9, 17, 2));
        });
    }

    @Test
    void 방_관리_토큰은_방_생성_삭제_권한만_담는다() {
        // when
        String token = liveKitTokenProvider.createRoomManageToken();

        // then
        Jws<Claims> jws = parse(token);
        Claims claims = jws.getBody();

        assertSoftly(softly -> {
            softly.assertThat(jws.getHeader().getAlgorithm()).isEqualTo("HS256");
            softly.assertThat(claims.getIssuer()).isEqualTo(API_KEY);
            softly.assertThat(claims.get("video", Map.class)).isEqualTo(Map.of("roomCreate", true));
        });
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(API_SECRET.getBytes(StandardCharsets.UTF_8))
                .setClock(() -> Date.from(NOW))
                .build()
                .parseClaimsJws(token);
    }
}
