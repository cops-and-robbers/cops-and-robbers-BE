package com.team.cops_and_robbers.community.chat.pin.application.dto.command;

public record CommunityChatPinDeleteCommand(
        Long userId,
        Long postId
) {
    public static CommunityChatPinDeleteCommand of(Long userId, Long postId) {
        return new CommunityChatPinDeleteCommand(userId, postId);
    }
}
