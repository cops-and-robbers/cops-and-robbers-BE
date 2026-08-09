package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.CommunityPostResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

public record CommunityPostListResponse(
        @Schema(description = "게시글 목록")
        List<CommunityPostResponse> content,
        @Schema(description = "페이지 정보")
        PageInfo page
) {
    public record PageInfo(
            int size,
            int number,
            long totalElements,
            int totalPages
    ) {
    }

    public static CommunityPostListResponse from(Page<CommunityPostResult> page) {
        List<CommunityPostResponse> content = page.getContent().stream()
                .map(CommunityPostResponse::from)
                .toList();
        PageInfo pageInfo = new PageInfo(
                page.getSize(),
                page.getNumber(),
                page.getTotalElements(),
                page.getTotalPages()
        );
        return new CommunityPostListResponse(content, pageInfo);
    }
}
