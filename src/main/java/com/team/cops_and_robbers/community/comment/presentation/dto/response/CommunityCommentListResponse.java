package com.team.cops_and_robbers.community.comment.presentation.dto.response;

import com.team.cops_and_robbers.community.comment.application.dto.result.CommunityCommentListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CommunityCommentListResponse(
        @Schema(description = "1depth 댓글 목록 (오래된 순). 각 댓글의 답글은 replies에 모두 담긴다")
        List<CommunityCommentResponse> content,
        @Schema(description = "다음 페이지 요청에 사용할 커서 (마지막 페이지면 null)",
                example = "12", nullable = true)
        Long nextCursor,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
    public static CommunityCommentListResponse from(CommunityCommentListResult result) {
        return new CommunityCommentListResponse(
                result.content().stream().map(CommunityCommentResponse::from).toList(),
                result.nextCursor(),
                result.hasNext()
        );
    }
}
