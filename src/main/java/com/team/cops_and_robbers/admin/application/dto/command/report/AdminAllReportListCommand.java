package com.team.cops_and_robbers.admin.application.dto.command.report;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.report.domain.ReportSource;
import com.team.cops_and_robbers.report.domain.ReportStatus;

public record AdminAllReportListCommand(
        int page,
        int size,
        ReportStatus status,
        ReportSource source,
        SortDirection sortDirection
) {

    public long offset() {
        return (long) page * size;
    }
}
