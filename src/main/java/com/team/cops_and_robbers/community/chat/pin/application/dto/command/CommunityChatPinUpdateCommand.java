package com.team.cops_and_robbers.community.chat.pin.application.dto.command;

public record CommunityChatPinUpdateCommand(
        Long userId,
        Long postId,
        String content
) {
    public static CommunityChatPinUpdateCommand of(Long userId, Long postId, String content) {
        return new CommunityChatPinUpdateCommand(userId, postId, content);
    }
}
