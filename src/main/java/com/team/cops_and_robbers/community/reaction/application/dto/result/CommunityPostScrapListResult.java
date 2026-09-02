package com.team.cops_and_robbers.community.reaction.application.dto.result;

import com.team.cops_and_robbers.community.post.application.dto.result.CommunityPostResult;

import java.util.List;

public record CommunityPostScrapListResult(
        List<CommunityPostResult> content,
        Long nextCursor,
        boolean hasNext
) {
}
