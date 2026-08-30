package com.team.cops_and_robbers.notice.presentation.dto.request;

import com.team.cops_and_robbers.notice.application.dto.command.NoticeCreateCommand;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import com.team.cops_and_robbers.notice.domain.NoticeLanguage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record NoticeCreateRequest(
        @Schema(description = "상단 고정 여부", example = "false")
        @NotNull(message = "고정 여부는 필수 입력 항목입니다.")
        Boolean pinned,

        @Schema(description = "카테고리 (생략 시 NOTICE)", example = "NOTICE")
        NoticeCategory category,

        @Schema(description = "원문 언어 코드 (ko·ja·en). translations 에 이 언어가 포함되어야 한다", example = "ko")
        @NotBlank(message = "원문 언어는 필수 입력 항목입니다.")
        @Pattern(regexp = "(?i)ko|ja|en", message = "언어는 ko, ja, en 중 하나여야 합니다.")
        String originalLanguage,

        @Schema(description = "언어별 제목·내용 목록")
        @NotEmpty(message = "번역은 최소 한 개 필요합니다.")
        @Valid
        List<NoticeTranslationRequest> translations
) {
    public NoticeCreateCommand toCommand(Long userId) {
        return new NoticeCreateCommand(
                userId,
                pinned,
                category != null ? category : NoticeCategory.NOTICE,
                NoticeLanguage.from(originalLanguage).orElseThrow(),
                translations.stream().map(NoticeTranslationRequest::toCommand).toList()
        );
    }
}
