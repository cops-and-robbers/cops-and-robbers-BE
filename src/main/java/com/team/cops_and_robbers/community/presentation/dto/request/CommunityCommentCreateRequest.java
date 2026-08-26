package com.team.cops_and_robbers.community.presentation.dto.request;

import com.team.cops_and_robbers.community.application.dto.command.CommunityCommentCreateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityCommentCreateRequest(
        @Schema(description = "답글을 달 댓글 ID. 일반 댓글이면 생략한다. 답글에는 답글을 달 수 없다.",
                example = "1", nullable = true)
        Long parentId,

        @Schema(description = "댓글 내용", example = "몇 시에 만나나요?")
        @NotBlank(message = "댓글 내용은 필수 입력 항목입니다.")
        @Size(max = 500, message = "댓글은 최대 500자 입니다.")
        String content
) {
    public CommunityCommentCreateCommand toCommand(Long writerId, Long postId) {
        return CommunityCommentCreateCommand.of(writerId, postId, parentId, content);
    }
}
