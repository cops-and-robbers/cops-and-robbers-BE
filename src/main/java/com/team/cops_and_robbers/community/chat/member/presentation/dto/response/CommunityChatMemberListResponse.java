package com.team.cops_and_robbers.community.chat.member.presentation.dto.response;

import com.team.cops_and_robbers.community.chat.member.application.dto.result.CommunityChatMemberListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CommunityChatMemberListResponse(
        @Schema(description = "이 방의 푸시 알림 수신 여부 (기본 true)", example = "true")
        boolean notificationEnabled,
        @Schema(description = "채팅방 멤버 목록")
        List<MemberResponse> members
) {
    public record MemberResponse(
            @Schema(description = "유저 ID", example = "7")
            Long userId,
            @Schema(description = "닉네임 (탈퇴한 유저면 \"알수없음\")", example = "무서운경찰관")
            String nickname,
            @Schema(description = "프로필 아이콘 번호 (탈퇴한 유저면 기본 아이콘 번호)", example = "1")
            int profileIcon,
            @Schema(description = "게시글 작성자(방장) 여부", example = "true")
            boolean isAuthor
    ) {
        public static MemberResponse from(CommunityChatMemberListResult.Member member) {
            return new MemberResponse(member.userId(), member.nickname(), member.profileIcon(), member.isAuthor());
        }
    }

    public static CommunityChatMemberListResponse from(CommunityChatMemberListResult result) {
        return new CommunityChatMemberListResponse(
                result.notificationEnabled(),
                result.members().stream().map(MemberResponse::from).toList());
    }
}
