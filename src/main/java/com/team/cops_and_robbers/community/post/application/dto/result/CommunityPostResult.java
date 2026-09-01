package com.team.cops_and_robbers.community.post.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.notification.application.dto.result.CommunityPostNotificationSettingResult;
import com.team.cops_and_robbers.community.post.domain.CommunityPost;
import com.team.cops_and_robbers.community.post.domain.RecruitmentStatus;
import com.team.cops_and_robbers.community.reaction.application.dto.CommunityPostReactionCounts;
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
        String updatedAt,
        boolean chatJoined,
        CommunityPostNotificationSettingResult notificationSettings,
        long likeCount,
        long scrapCount,
        boolean isLikedByRequester,
        boolean isScrappedByRequester
) {

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
     * chatJoined는 호출부에서 요청자 기준으로 계산해 넘긴다. 비로그인 조회는 false로 넘어온다.
     * notificationSettings는 단건 조회에서만 채운다. 목록에서는 쓰지 않아 null이다.
     * reaction도 호출부에서 요청자 기준으로 계산해 넘긴다.
     */
    public static CommunityPostResult from(
            CommunityPost post, User writer, boolean chatJoined, CommunityPostReactionCounts reaction) {
        return of(post, writer, chatJoined, null, reaction);
    }

    public static CommunityPostResult of(
            CommunityPost post,
            User writer,
            boolean chatJoined,
            CommunityPostNotificationSettingResult notificationSettings,
            CommunityPostReactionCounts reaction
    ) {
        return new CommunityPostResult(
                post.getId(),
                post.getWriterId(),
                writer != null ? writer.getNickname() : User.UNKNOWN_NICKNAME,
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
                TimestampUtil.toIsoString(post.getUpdatedAt()),
                chatJoined,
                notificationSettings,
                reaction.likeCount(),
                reaction.scrapCount(),
                reaction.isLikedByRequester(),
                reaction.isScrappedByRequester()
        );
    }
}
