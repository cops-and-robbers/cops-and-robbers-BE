package com.team.cops_and_robbers.play.radio.application.dto.result;

import java.time.LocalDateTime;

public record RadioTokenResult(
        String url,
        String room,
        String identity,
        String token,
        LocalDateTime expiresAt
) {
    public static RadioTokenResult of(
            String url, String room, Long participantId, String token, LocalDateTime expiresAt
    ) {
        return new RadioTokenResult(url, room, participantId.toString(), token, expiresAt);
    }
}
