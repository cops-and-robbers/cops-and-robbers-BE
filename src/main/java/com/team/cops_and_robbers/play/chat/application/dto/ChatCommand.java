package com.team.cops_and_robbers.play.chat.application.dto;

import com.team.cops_and_robbers.play.chat.domain.ChatScope;

public record ChatCommand(
        Long gameId,
        Long userId,
        Long participantId,
        String message,
        ChatScope scope
) {
}
