package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.command.report.AdminReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminUpdateReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminCommunityPostReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminCommunityPostReportResult;
import com.team.cops_and_robbers.report.domain.CommunityPostReport;
import com.team.cops_and_robbers.report.repository.CommunityPostReportRepository;
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
public class AdminCommunityPostReportService {

    private final CommunityPostReportRepository communityPostReportRepository;
    private final UserRepository userRepository;

    public AdminCommunityPostReportPageResult getCommunityPostReportList(AdminReportListCommand command) {
        Page<CommunityPostReport> reportPage = communityPostReportRepository.findAllForAdmin(command.status(), command.toPageable());

        Set<Long> userIds = reportPage.getContent().stream()
                .flatMap(report -> Stream.of(report.getReporterUserId(), report.getReportedUserId()))
                .collect(Collectors.toSet());

        Map<Long, String> nicknameByUserId = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        Page<AdminCommunityPostReportResult> resultPage = reportPage.map(report -> AdminCommunityPostReportResult.of(
                report,
                nicknameByUserId.get(report.getReporterUserId()),
                nicknameByUserId.get(report.getReportedUserId())
        ));

        return AdminCommunityPostReportPageResult.from(resultPage);
    }

    @Transactional
    public AdminCommunityPostReportResult updateCommunityPostReportStatus(AdminUpdateReportStatusCommand command) {
        CommunityPostReport report = communityPostReportRepository.getById(command.reportId());
        report.updateStatus(command.status(), command.adminMemo());

        Map<Long, String> nicknameByUserId = userRepository
                .findAllById(Set.of(report.getReporterUserId(), report.getReportedUserId()))
                .stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return AdminCommunityPostReportResult.of(
                report,
                nicknameByUserId.get(report.getReporterUserId()),
                nicknameByUserId.get(report.getReportedUserId())
        );
    }
}
