package com.team.cops_and_robbers.notice.presentation.dto.response;

import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import io.swagger.v3.oas.annotations.media.Schema;

public record NoticeResponse(
        @Schema(description = "공지사항 ID", example = "1")
        Long id,
        @Schema(description = "제목", example = "공지사항 제목")
        String title,
        @Schema(description = "내용", example = "공지사항 내용")
        String content,
        @Schema(description = "본문의 실제 언어 코드. 요청한 언어의 번역이 없으면 대체된 언어가 내려간다", example = "ja")
        String language,
        @Schema(description = "요청한 언어 코드. language 와 다르면 요청한 언어의 번역이 아직 없다는 뜻", example = "ja")
        String requestedLanguage,
        @Schema(description = "고정 여부", example = "false")
        boolean pinned,
        @Schema(description = "카테고리", example = "NOTICE")
        NoticeCategory category,
        @Schema(description = "생성일시", example = "2024-01-01T00:00:00+09:00")
        String createdAt,
        @Schema(description = "수정일시", example = "2024-01-01T00:00:00+09:00")
        String updatedAt
) {
    public static NoticeResponse from(NoticeResult result) {
        return new NoticeResponse(
                result.id(),
                result.title(),
                result.content(),
                result.language(),
                result.requestedLanguage(),
                result.pinned(),
                result.category(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
