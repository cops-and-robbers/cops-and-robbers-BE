package com.team.cops_and_robbers.notice.presentation.dto.request;

import com.team.cops_and_robbers.notice.application.dto.command.NoticeUpdateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeUpdateRequest(
        @Schema(description = "공지사항 제목", example = "서비스 점검 안내 (수정)")
        @NotBlank(message = "제목은 필수 입력 항목입니다.")
        @Size(max = 100, message = "제목은 최대 100자 입니다.")
        String title,

        @Schema(description = "공지사항 내용", example = "서버 점검 시간이 변경되었습니다.")
        @NotBlank(message = "내용은 필수 입력 항목입니다.")
        String content,

        @Schema(description = "상단 고정 여부", example = "true")
        @NotNull(message = "고정 여부는 필수 입력 항목입니다.")
        Boolean pinned
) {
    public NoticeUpdateCommand toCommand(Long userId, Long noticeId) {
        return new NoticeUpdateCommand(userId, noticeId, title, content, pinned);
    }
}
