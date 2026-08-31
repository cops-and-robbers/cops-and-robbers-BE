package com.team.cops_and_robbers.admin.application.dto.result.report;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminCommunityChatReportPageResult(
        List<AdminCommunityChatReportResult> content,
        Long totalElements,
        Integer totalPages,
        Integer page,
        Integer size
) {

    public static AdminCommunityChatReportPageResult from(Page<AdminCommunityChatReportResult> page) {
        return new AdminCommunityChatReportPageResult(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
