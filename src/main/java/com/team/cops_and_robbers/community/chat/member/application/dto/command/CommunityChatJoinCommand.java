package com.team.cops_and_robbers.community.chat.member.application.dto.command;

public record CommunityChatJoinCommand(
        Long userId,
        Long postId
) {
    public static CommunityChatJoinCommand of(Long userId, Long postId) {
        return new CommunityChatJoinCommand(userId, postId);
    }
}