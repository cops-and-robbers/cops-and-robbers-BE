package com.team.cops_and_robbers.community.presentation.dto.request;

import com.team.cops_and_robbers.community.application.dto.command.CommunityCommentNotificationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CommunityCommentNotificationRequest(
        @Schema(description = "이 댓글에 달리는 답글 알림을 받을지 여부", example = "false")
        @NotNull(message = "답글 알림 수신 여부는 필수 입력 항목입니다.")
        Boolean replyNotificationsEnabled
) {
    public CommunityCommentNotificationCommand toCommand(Long writerId, Long commentId) {
        return CommunityCommentNotificationCommand.of(writerId, commentId, replyNotificationsEnabled);
    }
}
