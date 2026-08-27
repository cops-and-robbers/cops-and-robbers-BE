package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.CommunityNotificationResult;
import com.team.cops_and_robbers.community.domain.CommunityNotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityNotificationResponse(
        @Schema(description = "알림 ID", example = "30")
        Long id,
        @Schema(description = "알림 종류. COMMENT는 내 글에 달린 댓글, REPLY는 답글", example = "COMMENT")
        CommunityNotificationType type,
        @Schema(description = "알림을 누르면 이동할 게시글 ID", example = "1")
        Long communityPostId,
        @Schema(description = "알림이 생긴 시점의 게시글 제목", example = "같이 경찰과 도둑 하실 분!")
        String postTitle,
        @Schema(description = "알림이 생긴 시점의 댓글 내용", example = "몇 시에 만나나요?")
        String content,
        @Schema(description = "읽음 여부", example = "false")
        boolean read,
        @Schema(description = "알림 발생 일시", example = "2026-08-27T12:00:00+09:00")
        String createdAt
) {
    public static CommunityNotificationResponse from(CommunityNotificationResult result) {
        return new CommunityNotificationResponse(
                result.id(),
                result.type(),
                result.communityPostId(),
                result.postTitle(),
                result.content(),
                result.read(),
                result.createdAt()
        );
    }
}
