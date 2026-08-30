package com.team.cops_and_robbers.notice.presentation.dto.response;

import com.team.cops_and_robbers.notice.application.dto.result.NoticeTranslationsResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record NoticeTranslationsResponse(
        @Schema(description = "공지사항 ID", example = "1")
        Long noticeId,
        @Schema(description = "원문 언어 코드", example = "ko")
        String originalLanguage,
        @Schema(description = "저장된 번역 전체")
        List<TranslationResponse> translations
) {
    public record TranslationResponse(
            @Schema(description = "언어 코드", example = "ja")
            String language,
            @Schema(description = "해당 언어의 제목", example = "サーバーメンテナンスのお知らせ")
            String title,
            @Schema(description = "해당 언어의 내용", example = "サーバーメンテナンスを実施します。")
            String content
    ) {
        public static TranslationResponse from(NoticeTranslationsResult.TranslationResult result) {
            return new TranslationResponse(result.language(), result.title(), result.content());
        }
    }

    public static NoticeTranslationsResponse from(NoticeTranslationsResult result) {
        return new NoticeTranslationsResponse(
                result.noticeId(),
                result.originalLanguage(),
                result.translations().stream().map(TranslationResponse::from).toList()
        );
    }
}
