package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.command.bug.AdminBugReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.bug.AdminUpdateBugReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.bug.AdminBugReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.bug.AdminBugReportResult;
import com.team.cops_and_robbers.bug.domain.BugReport;
import com.team.cops_and_robbers.bug.repository.BugReportRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBugReportService {

    private final BugReportRepository bugReportRepository;
    private final UserRepository userRepository;

    public AdminBugReportPageResult getBugReportList(AdminBugReportListCommand command) {
        Page<BugReport> bugReportPage = bugReportRepository.findAllForAdmin(command.status(), command.toPageable());

        Set<Long> userIds = bugReportPage.getContent().stream()
                .map(BugReport::getUserId)
                .collect(Collectors.toSet());

        Map<Long, String> nicknameByUserId = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        Page<AdminBugReportResult> resultPage = bugReportPage.map(bugReport ->
                AdminBugReportResult.of(bugReport, nicknameByUserId.get(bugReport.getUserId())));

        return AdminBugReportPageResult.from(resultPage);
    }

    @Transactional
    public AdminBugReportResult updateBugReportStatus(AdminUpdateBugReportStatusCommand command) {
        BugReport bugReport = bugReportRepository.getById(command.bugReportId());
        bugReport.updateStatus(command.status(), command.adminMemo());

        String nickname = userRepository.findAllById(List.of(bugReport.getUserId()))
                .stream()
                .findFirst()
                .map(User::getNickname)
                .orElse(null);

        return AdminBugReportResult.of(bugReport, nickname);
    }
}
