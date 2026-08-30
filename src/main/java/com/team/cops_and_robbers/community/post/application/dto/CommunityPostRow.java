package com.team.cops_and_robbers.community.post.application.dto;

import com.team.cops_and_robbers.community.post.domain.CommunityPost;

/**
 * distance는 거리순, score는 인기순일 때만 채워진다.
 */
public record CommunityPostRow(
        CommunityPost post,
        Double distance,
        Long score
) {
}
