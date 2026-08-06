package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.command.AdminReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.AdminUpdateReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.AdminReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.AdminReportResult;
import com.team.cops_and_robbers.report.domain.ChatReport;
import com.team.cops_and_robbers.report.repository.ReportRepository;
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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public AdminReportPageResult getReportList(AdminReportListCommand command) {
        Page<ChatReport> reportPage = reportRepository.findAllForAdmin(command.status(), command.toPageable());

        Set<Long> userIds = reportPage.getContent().stream()
                .flatMap(r -> Stream.of(r.getReporterUserId(), r.getReportedUserId()))
                .collect(Collectors.toSet());

        Map<Long, String> nicknameByUserId = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        Page<AdminReportResult> resultPage = reportPage.map(report -> AdminReportResult.of(
                report,
                nicknameByUserId.get(report.getReporterUserId()),
                nicknameByUserId.get(report.getReportedUserId())
        ));

        return AdminReportPageResult.from(resultPage);
    }

    @Transactional
    public AdminReportResult updateReportStatus(AdminUpdateReportStatusCommand command) {
        ChatReport report = reportRepository.getById(command.reportId());
        report.updateStatus(command.status(), command.adminId(), command.adminMemo());

        Map<Long, String> nicknameByUserId = userRepository
                .findAllById(List.of(report.getReporterUserId(), report.getReportedUserId()))
                .stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return AdminReportResult.of(
                report,
                nicknameByUserId.get(report.getReporterUserId()),
                nicknameByUserId.get(report.getReportedUserId())
        );
    }
}
