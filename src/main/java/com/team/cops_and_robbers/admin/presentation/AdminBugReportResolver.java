package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.admin.application.AdminBugReportService;
import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.AdminBugReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.AdminUpdateBugReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.AdminBugReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.AdminBugReportResult;
import com.team.cops_and_robbers.bug.domain.BugReportStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Validated
@Controller
@RequiredArgsConstructor
public class AdminBugReportResolver {

    private final AdminBugReportService adminBugReportService;

    @QueryMapping
    public AdminBugReportPageResult adminBugReports(
            @Argument @Min(0) int page,
            @Argument @Min(1) @Max(100) int size,
            @Argument BugReportStatus status,
            @Argument SortDirection sortDirection
    ) {
        AdminBugReportListCommand command = new AdminBugReportListCommand(
                page, size, status, sortDirection != null ? sortDirection : SortDirection.DESC);
        return adminBugReportService.getBugReportList(command);
    }

    @MutationMapping
    public AdminBugReportResult updateBugReportStatus(
            @Argument Long bugReportId,
            @Argument BugReportStatus status,
            @Argument String adminMemo
    ) {
        AdminUpdateBugReportStatusCommand command = new AdminUpdateBugReportStatusCommand(
                bugReportId, status, adminMemo);
        return adminBugReportService.updateBugReportStatus(command);
    }
}
