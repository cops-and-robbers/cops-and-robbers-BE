package com.team.cops_and_robbers.community.application.dto.command;

import com.team.cops_and_robbers.community.domain.RecruitmentStatus;

public record CommunityPostStatusCommand(
        Long userId,
        Long postId,
        RecruitmentStatus status
) {
}
