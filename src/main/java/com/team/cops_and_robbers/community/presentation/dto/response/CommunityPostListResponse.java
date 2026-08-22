package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.CommunityPostCursorResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CommunityPostListResponse(
        @Schema(description = "게시글 목록")
        List<CommunityPostResponse> content,
        @Schema(description = "커서 정보")
        CursorInfo cursor
) {
    public record CursorInfo(
            @Schema(description = "다음 페이지 요청에 사용할 커서 (마지막 페이지면 null)",
                    example = "MjAyNi0wOC0xNVQxMjozMDo0NXw0Mg", nullable = true)
            String nextCursor,
            @Schema(description = "다음 페이지 존재 여부", example = "true")
            boolean hasNext
    ) {
    }

    public static CommunityPostListResponse from(CommunityPostCursorResult result) {
        List<CommunityPostResponse> content = result.content().stream()
                .map(CommunityPostResponse::from)
                .toList();
        CursorInfo cursorInfo = new CursorInfo(result.nextCursor(), result.hasNext());
        return new CommunityPostListResponse(content, cursorInfo);
    }
}
