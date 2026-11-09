package com.team.cops_and_robbers.play.radio.infrastructure;

import java.time.LocalDateTime;

public record RadioToken(String token, LocalDateTime expiresAt) {
}
