package com.team.cops_and_robbers.user.presentation.dto.response;

import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record MyPageResponse(
        @Schema(description = "유저 ID", example = "7")
        Long userId,
        @Schema(description = "닉네임", example = "민첩한괴도5308")
        String nickname,
        @Schema(description = "소셜 플랫폼", example = "KAKAO")
        SocialType socialPlatform,
        @Schema(description = "게임 알림 수신 여부", example = "true")
        boolean allowGamePush,
        @Schema(description = "마케팅 알림 수신 여부", example = "false")
        boolean allowMarketingPush,
        @Schema(description = "커뮤니티 알림 수신 여부", example = "true")
        boolean allowCommunityPush,
        @Schema(description = "프로필 아이콘 번호", example = "1")
        int profileIcon
) {
    public static MyPageResponse from(User user) {
        return new MyPageResponse(
                user.getId(),
                user.getNickname(),
                user.getSocialType(),
                user.isAllowGamePush(),
                user.isAllowMarketingPush(),
                user.isAllowCommunityPush(),
                user.getProfileIcon()
        );
    }
}
