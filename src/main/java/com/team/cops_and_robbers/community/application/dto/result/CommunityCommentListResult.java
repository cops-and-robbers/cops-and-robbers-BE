package com.team.cops_and_robbers.community.application.dto.result;

import java.util.List;

public record CommunityCommentListResult(
        List<CommunityCommentResult> content,
        Long nextCursor,
        boolean hasNext
) {
}
