package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.command.report.AdminReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminUpdateReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminCommunityChatReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminCommunityChatReportResult;
import com.team.cops_and_robbers.report.domain.CommunityChatReport;
import com.team.cops_and_robbers.report.repository.CommunityChatReportRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCommunityChatReportService {

    private final CommunityChatReportRepository communityChatReportRepository;
    private final UserRepository userRepository;

    public AdminCommunityChatReportPageResult getCommunityChatReportList(AdminReportListCommand command) {
        Page<CommunityChatReport> reportPage = communityChatReportRepository.findAllForAdmin(command.status(), command.toPageable());

        Set<Long> userIds = reportPage.getContent().stream()
                .flatMap(report -> Stream.of(report.getReporterUserId(), report.getReportedUserId()))
                .collect(Collectors.toSet());

        Map<Long, String> nicknameByUserId = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        Page<AdminCommunityChatReportResult> resultPage = reportPage.map(report -> AdminCommunityChatReportResult.of(
                report,
                nicknameByUserId.get(report.getReporterUserId()),
                nicknameByUserId.get(report.getReportedUserId())
        ));

        return AdminCommunityChatReportPageResult.from(resultPage);
    }

    @Transactional
    public AdminCommunityChatReportResult updateCommunityChatReportStatus(AdminUpdateReportStatusCommand command) {
        CommunityChatReport report = communityChatReportRepository.getById(command.reportId());
        report.updateStatus(command.status(), command.adminMemo());

        Map<Long, String> nicknameByUserId = userRepository
                .findAllById(Set.of(report.getReporterUserId(), report.getReportedUserId()))
                .stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return AdminCommunityChatReportResult.of(
                report,
                nicknameByUserId.get(report.getReporterUserId()),
                nicknameByUserId.get(report.getReportedUserId())
        );
    }
}
