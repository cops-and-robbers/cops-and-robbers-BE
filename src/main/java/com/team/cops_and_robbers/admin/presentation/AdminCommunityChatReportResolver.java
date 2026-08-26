package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.admin.application.AdminCommunityChatReportService;
import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminUpdateReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminCommunityChatReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminCommunityChatReportResult;
import com.team.cops_and_robbers.report.domain.ReportStatus;
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
public class AdminCommunityChatReportResolver {

    private final AdminCommunityChatReportService adminCommunityChatReportService;

    @QueryMapping
    public AdminCommunityChatReportPageResult adminCommunityChatReports(
            @Argument @Min(0) int page,
            @Argument @Min(1) @Max(100) int size,
            @Argument ReportStatus status,
            @Argument SortDirection sortDirection
    ) {
        AdminReportListCommand command = new AdminReportListCommand(
                page, size, status, sortDirection != null ? sortDirection : SortDirection.DESC);
        return adminCommunityChatReportService.getCommunityChatReportList(command);
    }

    @MutationMapping
    public AdminCommunityChatReportResult updateCommunityChatReportStatus(
            @Argument Long reportId,
            @Argument ReportStatus status,
            @Argument String adminMemo
    ) {
        AdminUpdateReportStatusCommand command = new AdminUpdateReportStatusCommand(
                reportId, status, adminMemo);
        return adminCommunityChatReportService.updateCommunityChatReportStatus(command);
    }
}
