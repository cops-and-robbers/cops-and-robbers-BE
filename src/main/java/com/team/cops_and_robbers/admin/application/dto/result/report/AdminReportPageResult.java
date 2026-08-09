package com.team.cops_and_robbers.admin.application.dto.result.report;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminReportPageResult(
        List<AdminReportResult> content,
        Long totalElements,
        Integer totalPages,
        Integer page,
        Integer size
) {

    public static AdminReportPageResult from(Page<AdminReportResult> page) {
        return new AdminReportPageResult(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
