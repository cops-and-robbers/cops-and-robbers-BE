package com.team.cops_and_robbers.community.post.application.dto.command;

import com.team.cops_and_robbers.community.post.domain.RecruitmentStatus;

public record CommunityPostStatusCommand(
        Long writerId,
        Long postId,
        RecruitmentStatus status
) {
}
