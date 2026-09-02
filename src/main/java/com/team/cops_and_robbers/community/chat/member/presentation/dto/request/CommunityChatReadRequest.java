package com.team.cops_and_robbers.community.chat.member.presentation.dto.request;

import com.team.cops_and_robbers.community.chat.member.application.dto.command.CommunityChatReadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CommunityChatReadRequest(
        @Schema(description = "읽은 마지막 메시지 ID. 보통 화면에 보인 가장 최신 메시지의 id", example = "1234")
        @NotNull(message = "읽은 마지막 메시지 ID는 필수 입력 항목입니다.")
        Long lastReadMessageId
) {
    public CommunityChatReadCommand toCommand(Long userId, Long postId) {
        return CommunityChatReadCommand.of(userId, postId, lastReadMessageId);
    }
}
