package com.team.cops_and_robbers.community.application.dto.command;

public record CommunityChatPinRegisterCommand(
        Long userId,
        Long postId,
        String content
) {
    public static CommunityChatPinRegisterCommand of(Long userId, Long postId, String content) {
        return new CommunityChatPinRegisterCommand(userId, postId, content);
    }
}
