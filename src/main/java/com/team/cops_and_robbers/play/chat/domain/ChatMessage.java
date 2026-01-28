package com.team.cops_and_robbers.play.chat.domain;

import com.team.cops_and_robbers.common.util.TimestampUtil;

import java.util.UUID;

public record ChatMessage(
        String id,
        Long gameId,
        ChatSender sender,
        String message,
        String timestamp,
        ChatScope scope
) {
    public static ChatMessage of(Long gameId, ChatSender sender, String message, ChatScope scope) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                gameId,
                sender,
                message,
                TimestampUtil.nowKstIso(),
                scope
        );
    }
}
