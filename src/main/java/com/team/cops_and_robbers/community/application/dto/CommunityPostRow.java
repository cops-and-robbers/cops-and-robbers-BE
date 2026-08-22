package com.team.cops_and_robbers.community.application.dto;

import com.team.cops_and_robbers.community.domain.CommunityPost;

/** distance는 거리순일 때만 채워진다. 커서에 넣을 값을 쿼리에서 그대로 받아 자바 계산과 어긋나지 않게 한다. */
public record CommunityPostRow(
        CommunityPost post,
        Double distance
) {
}
