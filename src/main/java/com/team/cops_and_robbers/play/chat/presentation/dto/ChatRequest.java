package com.team.cops_and_robbers.play.chat.presentation.dto;

import com.team.cops_and_robbers.play.chat.application.dto.ChatCommand;
import com.team.cops_and_robbers.play.chat.domain.ChatScope;

public record ChatRequest(
        String message,
        ChatScope scope
) {
    public ChatCommand toCommand(Long gameId, Long userId, Long participantId) {
        return new ChatCommand(
                gameId,
                userId,
                participantId,
                message,
                scope
        );
    }
}
