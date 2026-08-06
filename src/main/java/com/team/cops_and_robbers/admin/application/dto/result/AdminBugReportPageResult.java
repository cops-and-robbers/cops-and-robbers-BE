package com.team.cops_and_robbers.admin.application.dto.result;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminBugReportPageResult(
        List<AdminBugReportResult> content,
        Long totalElements,
        Integer totalPages,
        Integer page,
        Integer size
) {

    public static AdminBugReportPageResult from(Page<AdminBugReportResult> page) {
        return new AdminBugReportPageResult(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
