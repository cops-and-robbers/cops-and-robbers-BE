package com.team.cops_and_robbers.community.reaction.presentation.dto.response;

import com.team.cops_and_robbers.community.post.presentation.dto.response.CommunityPostResponse;
import com.team.cops_and_robbers.community.reaction.application.dto.result.CommunityPostScrapListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CommunityPostScrapListResponse(
        @Schema(description = "스크랩한 게시글 목록 (스크랩한 순서, 최신순)")
        List<CommunityPostResponse> content,
        @Schema(description = "다음 페이지 요청에 사용할 커서 (마지막 페이지면 null)", example = "12", nullable = true)
        Long nextCursor,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
    public static CommunityPostScrapListResponse from(CommunityPostScrapListResult result) {
        return new CommunityPostScrapListResponse(
                result.content().stream().map(CommunityPostResponse::from).toList(),
                result.nextCursor(),
                result.hasNext()
        );
    }
}
