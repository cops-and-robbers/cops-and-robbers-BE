package com.team.cops_and_robbers.community.chat.pin.presentation.dto.request;

import com.team.cops_and_robbers.community.chat.pin.application.dto.command.CommunityChatPinUpdateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityChatPinUpdateRequest(
        @Schema(description = "고정 채팅 내용", example = "장소가 후문으로 변경되었습니다!")
        @NotBlank(message = "고정 채팅 내용을 입력해주세요.")
        @Size(max = 500, message = "고정 채팅 내용은 500자 이하로 입력해주세요.")
        String content
) {
    public CommunityChatPinUpdateCommand toCommand(Long userId, Long postId) {
        return CommunityChatPinUpdateCommand.of(userId, postId, content);
    }
}
