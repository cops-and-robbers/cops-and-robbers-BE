package com.team.cops_and_robbers.admin.application.dto.command;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record AdminReportListCommand(
        int page,
        int size,
        ReportStatus status,
        SortDirection sortDirection
) {

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(sortDirection.toSpringDirection(), "createdAt"));
    }
}
