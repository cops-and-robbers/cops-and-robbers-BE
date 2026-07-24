package com.team.cops_and_robbers.admin.application.dto.result;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminGamePageResult(
        List<AdminGameSummaryResult> content,
        Long totalElements,
        Integer totalPages,
        Integer page,
        Integer size
) {
    public static AdminGamePageResult from(Page<AdminGameSummaryResult> summaryPage) {
        return new AdminGamePageResult(
                summaryPage.getContent(),
                summaryPage.getTotalElements(),
                summaryPage.getTotalPages(),
                summaryPage.getNumber(),
                summaryPage.getSize()
        );
    }
}
