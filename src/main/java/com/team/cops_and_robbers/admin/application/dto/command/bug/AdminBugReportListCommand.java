package com.team.cops_and_robbers.admin.application.dto.command.bug;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.bug.domain.BugReportStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record AdminBugReportListCommand(
        int page,
        int size,
        BugReportStatus status,
        SortDirection sortDirection
) {

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(sortDirection.toSpringDirection(), "createdAt"));
    }
}
