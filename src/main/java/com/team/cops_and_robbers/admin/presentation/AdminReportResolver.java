package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.admin.application.AdminReportService;
import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.AdminReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.AdminUpdateReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.AdminReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.AdminReportResult;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import jakarta.servlet.http.HttpServletRequest;
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
public class AdminReportResolver {

    private final AdminReportService adminReportService;

    @QueryMapping
    public AdminReportPageResult adminReports(
            @Argument @Min(0) int page,
            @Argument @Min(1) @Max(100) int size,
            @Argument ReportStatus status,
            @Argument SortDirection sortDirection
    ) {
        AdminReportListCommand command = new AdminReportListCommand(
                page, size, status, sortDirection != null ? sortDirection : SortDirection.DESC);
        return adminReportService.getReportList(command);
    }

    @MutationMapping
    public AdminReportResult updateReportStatus(
            @Argument Long reportId,
            @Argument ReportStatus status,
            @Argument String adminMemo,
            HttpServletRequest request
    ) {
        LoginUser loginUser = (LoginUser) request.getAttribute("loginUser");
        AdminUpdateReportStatusCommand command = new AdminUpdateReportStatusCommand(
                reportId, status, adminMemo, loginUser.userId());
        return adminReportService.updateReportStatus(command);
    }
}
