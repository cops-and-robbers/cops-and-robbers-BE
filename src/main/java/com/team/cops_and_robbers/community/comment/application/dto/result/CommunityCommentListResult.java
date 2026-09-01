package com.team.cops_and_robbers.community.comment.application.dto.result;

import java.util.List;

public record CommunityCommentListResult(
        List<CommunityCommentResult> content,
        Long nextCursor,
        boolean hasNext
) {
}
