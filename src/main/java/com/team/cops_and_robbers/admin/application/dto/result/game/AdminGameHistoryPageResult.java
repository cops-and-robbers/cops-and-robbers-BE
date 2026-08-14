package com.team.cops_and_robbers.admin.application.dto.result.game;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminGameHistoryPageResult(
        List<AdminGameHistoryResult> content,
        Long totalElements,
        Integer totalPages,
        Integer page,
        Integer size
) {
    public static AdminGameHistoryPageResult from(Page<AdminGameHistoryResult> historyPage) {
        return new AdminGameHistoryPageResult(
                historyPage.getContent(),
                historyPage.getTotalElements(),
                historyPage.getTotalPages(),
                historyPage.getNumber(),
                historyPage.getSize()
        );
    }
}
