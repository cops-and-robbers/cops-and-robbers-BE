package com.team.cops_and_robbers.notice.presentation.dto.request;

import com.team.cops_and_robbers.notice.application.dto.command.NoticeTranslationCommand;
import com.team.cops_and_robbers.notice.domain.NoticeLanguage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NoticeTranslationRequest(
        @Schema(description = "언어 코드 (ko·ja·en)", example = "ko")
        @NotBlank(message = "언어는 필수 입력 항목입니다.")
        @Pattern(regexp = "(?i)ko|ja|en", message = "언어는 ko, ja, en 중 하나여야 합니다.")
        String language,

        @Schema(description = "해당 언어의 제목", example = "서비스 점검 안내")
        @NotBlank(message = "제목은 필수 입력 항목입니다.")
        @Size(max = 100, message = "제목은 최대 100자 입니다.")
        String title,

        @Schema(description = "해당 언어의 내용", example = "서버 점검으로 인해 서비스가 일시 중단됩니다.")
        @NotBlank(message = "내용은 필수 입력 항목입니다.")
        String content
) {
    public NoticeTranslationCommand toCommand() {
        // @Pattern 검증을 통과한 값이라 항상 존재한다
        return new NoticeTranslationCommand(NoticeLanguage.from(language).orElseThrow(), title, content);
    }
}
