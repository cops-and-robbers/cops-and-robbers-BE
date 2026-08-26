package com.team.cops_and_robbers.admin.application.dto.result.report;

import java.util.List;

public record AdminAllReportPageResult(
        List<AdminAllReportResult> content,
        Long totalElements,
        Integer totalPages,
        Integer page,
        Integer size
) {

    public static AdminAllReportPageResult of(List<AdminAllReportResult> content, Long totalElements, Integer page, Integer size) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new AdminAllReportPageResult(content, totalElements, totalPages, page, size);
    }
}
