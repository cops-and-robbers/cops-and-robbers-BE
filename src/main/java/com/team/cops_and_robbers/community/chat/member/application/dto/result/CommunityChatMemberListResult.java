package com.team.cops_and_robbers.community.chat.member.application.dto.result;

import com.team.cops_and_robbers.user.domain.User;

import java.util.List;

public record CommunityChatMemberListResult(
        boolean notificationEnabled,
        List<Member> members
) {
    /**
     * user가 null이면 탈퇴한 유저다. 닉네임은 "알수없음", 아이콘은 기본 아이콘 번호로 채운다.
     */
    public record Member(
            Long userId,
            String nickname,
            int profileIcon,
            boolean isAuthor
    ) {
        public static Member of(Long userId, User user, Long writerId) {
            return new Member(
                    userId,
                    user != null ? user.getNickname() : User.UNKNOWN_NICKNAME,
                    user != null ? user.getProfileIcon() : User.DEFAULT_PROFILE_ICON,
                    userId.equals(writerId)
            );
        }
    }
}
