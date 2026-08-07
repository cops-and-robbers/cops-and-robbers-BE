package com.team.cops_and_robbers.community.application.dto.command;

public record CommunityPostDeleteCommand(
        Long userId,
        Long postId
) {
}
