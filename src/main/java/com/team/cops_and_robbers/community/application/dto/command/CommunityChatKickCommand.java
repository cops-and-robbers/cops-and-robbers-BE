package com.team.cops_and_robbers.community.application.dto.command;

public record CommunityChatKickCommand(
        Long hostId,
        Long postId,
        Long targetUserId
) {
    public static CommunityChatKickCommand of(Long hostId, Long postId, Long targetUserId) {
        return new CommunityChatKickCommand(hostId, postId, targetUserId);
    }
}
