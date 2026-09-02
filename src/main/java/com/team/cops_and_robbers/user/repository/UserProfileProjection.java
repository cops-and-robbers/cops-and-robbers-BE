package com.team.cops_and_robbers.user.repository;

import com.team.cops_and_robbers.user.domain.User;

public record UserProfileProjection(
        Long userId,
        String nickname,
        int profileIcon
) {
    /**
     * 참조 대상 유저가 탈퇴해 존재하지 않을 수 있는 자리에서 사용
     */
    public static UserProfileProjection of(Long userId, User user) {
        return user != null
                ? new UserProfileProjection(userId, user.getNickname(), user.getProfileIcon())
                : new UserProfileProjection(userId, User.UNKNOWN_NICKNAME, User.DEFAULT_PROFILE_ICON);
    }
}
