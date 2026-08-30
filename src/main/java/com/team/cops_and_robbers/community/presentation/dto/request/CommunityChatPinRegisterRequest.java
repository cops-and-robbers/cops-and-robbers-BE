package com.team.cops_and_robbers.community.presentation.dto.request;

import com.team.cops_and_robbers.community.application.dto.command.CommunityChatPinRegisterCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityChatPinRegisterRequest(
        @Schema(description = "고정 채팅 내용", example = "오늘 오후 7시 정문에서 만나요!")
        @NotBlank(message = "고정 채팅 내용을 입력해주세요.")
        @Size(max = 500, message = "고정 채팅 내용은 500자 이하로 입력해주세요.")
        String content
) {
    public CommunityChatPinRegisterCommand toCommand(Long userId, Long postId) {
        return CommunityChatPinRegisterCommand.of(userId, postId, content);
    }
}
