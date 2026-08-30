package com.team.cops_and_robbers.community.post.application.dto.result;

import java.util.List;

public record CommunityPostCursorResult(
        List<CommunityPostResult> content,
        String nextCursor,
        boolean hasNext
) {
}
