package com.team.cops_and_robbers.community.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import com.team.cops_and_robbers.user.domain.User;

public record CommunityPostResult(
        Long id,
        Long writerId,
        String writerNickname,
        int writerProfileIcon,
        String title,
        String content,
        String meetingAt,
        LocationResult location,
        Integer maxParticipants,
        RecruitmentStatus status,
        String createdAt,
        String updatedAt
) {

    private static final String UNKNOWN_USER = "알수없음";

    public record LocationResult(
            Double latitude,
            Double longitude,
            String region,
            String address,
            String placeName,
            String countryCode
    ) {
    }

    /**
     * 탈퇴한 작성자는 writer가 null로 들어온다.
     * 닉네임은 "알수없음"으로, 프로필 아이콘은 기본 아이콘 번호로 내려준다.
     */
    public static CommunityPostResult from(CommunityPost post, User writer) {
        return new CommunityPostResult(
                post.getId(),
                post.getWriterId(),
                writer != null ? writer.getNickname() : UNKNOWN_USER,
                writer != null ? writer.getProfileIcon() : User.DEFAULT_PROFILE_ICON,
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
