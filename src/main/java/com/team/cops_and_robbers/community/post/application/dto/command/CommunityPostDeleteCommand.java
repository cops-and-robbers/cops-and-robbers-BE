package com.team.cops_and_robbers.community.post.application.dto.command;

public record CommunityPostDeleteCommand(
        Long writerId,
        Long postId
) {
}
