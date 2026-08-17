package com.team.cops_and_robbers.community.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;

public record CommunityPostResult(
        Long id,
        Long writerId,
        String writerNickname,
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
            Double longitude,
            String region,
            String placeName
    ) {
    }

    public static CommunityPostResult from(CommunityPost post, String writerNickname) {
        return new CommunityPostResult(
                post.getId(),
                post.getWriterId(),
                writerNickname,
                post.getTitle(),
                post.getContent(),
                TimestampUtil.toIsoString(post.getMeetingAt()),
                new LocationResult(
                        post.getLatitude(),
                        post.getLongitude(),
                        post.getRegion(),
                        post.getPlaceName()),
                post.getMaxParticipants(),
                post.getStatus(),
                TimestampUtil.toIsoString(post.getCreatedAt()),
                TimestampUtil.toIsoString(post.getUpdatedAt())
        );
    }
}
