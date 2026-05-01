package com.team.cops_and_robbers.notice.presentation.dto.response;

import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

public record NoticeListResponse(
        @Schema(description = "공지사항 목록")
        List<NoticeResponse> content,
        @Schema(description = "페이지 정보")
        PageInfo page
) {
    public record PageInfo(
            int size,
            int number,
            long totalElements,
            int totalPages
    ) {}

    public static NoticeListResponse from(Page<NoticeResult> page) {
        List<NoticeResponse> content = page.getContent().stream()
                .map(NoticeResponse::from)
                .toList();
        PageInfo pageInfo = new PageInfo(
                page.getSize(),
                page.getNumber(),
                page.getTotalElements(),
                page.getTotalPages()
        );
        return new NoticeListResponse(content, pageInfo);
    }
}