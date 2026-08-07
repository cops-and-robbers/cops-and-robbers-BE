package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.admin.application.AdminDashboardService;
import com.team.cops_and_robbers.admin.application.dto.result.dashboard.AdminDashboardResult;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class AdminDashboardResolver {

    private final AdminDashboardService adminDashboardService;

    @QueryMapping
    public AdminDashboardResult adminDashboard() {
        return adminDashboardService.getDashboard();
    }
}
