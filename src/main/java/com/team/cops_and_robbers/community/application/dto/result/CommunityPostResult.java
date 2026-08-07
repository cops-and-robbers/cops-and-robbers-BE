package com.team.cops_and_robbers.community.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;

public record CommunityPostResult(
        Long id,
        Long userId,
        String title,
        String content,
        String meetingAt,
        LocationResult location,
        Integer maxParticipants,
        RecruitmentStatus status,
        String createdAt,
        String updatedAt
) {
    public record LocationResult(
            Double latitude,
            Double longitude
    ) {
    }

    public static CommunityPostResult from(CommunityPost post) {
        return new CommunityPostResult(
                post.getId(),
                post.getUserId(),
                post.getTitle(),
                post.getContent(),
                TimestampUtil.toIsoString(post.getMeetingAt()),
                new LocationResult(post.getLatitude(), post.getLongitude()),
                post.getMaxParticipants(),
                post.getStatus(),
                TimestampUtil.toIsoString(post.getCreatedAt()),
                TimestampUtil.toIsoString(post.getUpdatedAt())
        );
    }
}
