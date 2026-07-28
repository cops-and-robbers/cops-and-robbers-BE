package com.team.cops_and_robbers.auth.infrastructure.jwt;


import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final Key accessKey;
    private final Key refreshKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        byte[] accessKeyBytes = Decoders.BASE64.decode(jwtProperties.access().secret());
        this.accessKey = Keys.hmacShaKeyFor(accessKeyBytes);

        byte[] refreshKeyBytes = Decoders.BASE64.decode(jwtProperties.refresh().secret());
        this.refreshKey = Keys.hmacShaKeyFor(refreshKeyBytes);
    }


    /**
     * Access token logic
     */
    public String createAccessToken(User user) {
        Date now = new Date();
        long expirationMillis = jwtProperties.access().expiration().toMillis();
        Date expirationTime = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expirationTime)
                .signWith(accessKey)
                .compact();
    }

    public Long getUserIdFromAccessToken(String accessToken) {
        return Long.valueOf(
                getAccessClaims(accessToken)
                        .getSubject()
        );
    }

    public String getRoleFromAccessToken(String accessToken) {
        return getAccessClaims(accessToken).get("role", String.class);
    }

    private Claims getAccessClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(accessKey)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new ApplicationException(AuthException.ACCESS_TOKEN_EXPIRED);
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(AuthException.UNAUTHENTICATED_REQUEST);
        } catch (JwtException e) {
            throw new ApplicationException(AuthException.INVALID_TOKEN);
        }
    }


    /**
     * Refresh token logic
     */
    public String createRefreshToken(User user) {
        Date now = new Date();
        long expirationMillis = jwtProperties.refresh().expiration().toMillis();
        Date expirationTime = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(now)
                .setExpiration(expirationTime)
                .signWith(refreshKey)
                .compact();
    }

    public Long getUserIdFromRefreshToken(String refreshToken) {
        return Long.valueOf(
                getRefreshClaims(refreshToken)
                        .getSubject()
        );
    }



    private Claims getRefreshClaims(String refreshToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(refreshKey)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new ApplicationException(AuthException.REFRESH_TOKEN_EXPIRED);
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(AuthException.UNAUTHENTICATED_REQUEST);
        } catch (JwtException e) {
            throw new ApplicationException(AuthException.INVALID_TOKEN);
        }
    }

    public Optional<Long> getUserIdFromRefreshTokenLogout(String refreshToken) {
        return getRefreshClaimsOptional(refreshToken)
                .map(claims -> Long.valueOf(claims.getSubject()));
    }

    private Optional<Claims> getRefreshClaimsOptional(String refreshToken) {
        try {
            return Optional.of(Jwts.parserBuilder()
                    .setSigningKey(refreshKey)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody());
        } catch (ExpiredJwtException e) {
            return Optional.of(e.getClaims());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getRefreshTokenExpirationMillis() {
        return jwtProperties.refresh().expiration().toMillis();
    }

}
