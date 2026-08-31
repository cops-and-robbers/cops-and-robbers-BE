package com.team.cops_and_robbers.admin.application.dto.result.report;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminCommunityPostReportPageResult(
        List<AdminCommunityPostReportResult> content,
        Long totalElements,
        Integer totalPages,
        Integer page,
        Integer size
) {

    public static AdminCommunityPostReportPageResult from(Page<AdminCommunityPostReportResult> page) {
        return new AdminCommunityPostReportPageResult(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
