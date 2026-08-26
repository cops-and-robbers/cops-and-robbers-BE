package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.admin.application.AdminAllReportService;
import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminAllReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminAllReportPageResult;
import com.team.cops_and_robbers.report.domain.ReportSource;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Validated
@Controller
@RequiredArgsConstructor
public class AdminAllReportResolver {

    private final AdminAllReportService adminAllReportService;

    @QueryMapping
    public AdminAllReportPageResult adminAllReports(
            @Argument @Min(0) int page,
            @Argument @Min(1) @Max(100) int size,
            @Argument ReportStatus status,
            @Argument ReportSource source,
            @Argument SortDirection sortDirection
    ) {
        AdminAllReportListCommand command = new AdminAllReportListCommand(
                page, size, status, source, sortDirection != null ? sortDirection : SortDirection.DESC);
        return adminAllReportService.getAllReportList(command);
    }
}
