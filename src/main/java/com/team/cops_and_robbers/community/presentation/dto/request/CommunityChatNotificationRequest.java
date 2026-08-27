package com.team.cops_and_robbers.community.presentation.dto.request;

import com.team.cops_and_robbers.community.application.dto.command.CommunityChatNotificationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CommunityChatNotificationRequest(
        @Schema(description = "이 채팅방의 푸시 알림을 받을지 여부", example = "false")
        @NotNull(message = "채팅방 알림 수신 여부는 필수 입력 항목입니다.")
        Boolean allowNotification
) {
    public CommunityChatNotificationCommand toCommand(Long userId, Long postId) {
        return CommunityChatNotificationCommand.of(userId, postId, allowNotification);
    }
}
