package com.team.cops_and_robbers.community.presentation.dto.request;

import com.team.cops_and_robbers.community.application.dto.command.CommunityPostStatusCommand;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CommunityPostStatusRequest(
        @Schema(description = "모집 상태", example = "COMPLETED")
        @NotNull(message = "모집 상태는 필수 입력 항목입니다.")
        RecruitmentStatus status
) {
    public CommunityPostStatusCommand toCommand(Long writerId, Long postId) {
        return new CommunityPostStatusCommand(writerId, postId, status);
    }
}
