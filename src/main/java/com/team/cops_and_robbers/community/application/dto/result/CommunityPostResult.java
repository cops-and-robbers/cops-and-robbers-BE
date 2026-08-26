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

    private static final String WITHDRAWN_USER = "탈퇴한 사용자";

    public record LocationResult(
            Double latitude,
            Double longitude,
            String region,
            String address,
            String placeName,
            String countryCode
    ) {
    }

    public static CommunityPostResult from(CommunityPost post, String writerNickname) {
        return new CommunityPostResult(
                post.getId(),
                post.getWriterId(),
                writerNickname != null ? writerNickname : WITHDRAWN_USER,
                post.getTitle(),
                post.getContent(),
                TimestampUtil.toIsoString(post.getMeetingAt()),
                new LocationResult(
                        post.getLatitude(),
                        post.getLongitude(),
                        post.getRegion(),
                        post.getAddress(),
                        post.getPlaceName(),
                        post.getCountryCode()),
                post.getMaxParticipants(),
                post.currentStatus(),
                TimestampUtil.toIsoString(post.getCreatedAt()),
                TimestampUtil.toIsoString(post.getUpdatedAt())
        );
    }
}
