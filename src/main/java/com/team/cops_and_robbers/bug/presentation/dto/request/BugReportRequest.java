package com.team.cops_and_robbers.bug.presentation.dto.request;

import com.team.cops_and_robbers.bug.application.dto.command.BugReportCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BugReportRequest(
        @Schema(description = "버그 내용", example = "게임 시작 버튼을 누르면 앱이 꺼져요")
        @NotBlank(message = "버그 내용은 필수 입력 항목입니다.")
        @Size(max = 1000, message = "버그 내용은 최대 1000자입니다.")
        String content
) {
    public BugReportCommand toCommand(Long userId) {
        return new BugReportCommand(userId, content);
    }
}
