package com.team.cops_and_robbers.community.application.dto.command;

public record CommunityChatReadCommand(
        Long userId,
        Long postId,
        Long lastReadMessageId
) {
    public static CommunityChatReadCommand of(Long userId, Long postId, Long lastReadMessageId) {
        return new CommunityChatReadCommand(userId, postId, lastReadMessageId);
    }
}
