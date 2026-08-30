package com.team.cops_and_robbers.community.chat.member.application.dto.command;

public record CommunityChatLeaveCommand(
        Long userId,
        Long postId
) {
    public static CommunityChatLeaveCommand of(Long userId, Long postId) {
        return new CommunityChatLeaveCommand(userId, postId);
    }
}