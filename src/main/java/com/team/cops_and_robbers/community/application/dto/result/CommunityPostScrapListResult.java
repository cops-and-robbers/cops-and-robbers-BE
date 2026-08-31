package com.team.cops_and_robbers.community.application.dto.result;

import java.util.List;

public record CommunityPostScrapListResult(
        List<CommunityPostResult> content,
        Long nextCursor,
        boolean hasNext
) {
}
