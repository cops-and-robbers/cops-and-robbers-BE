package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminAllReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminAllReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminAllReportResult;
import com.team.cops_and_robbers.report.repository.AdminAllReportRow;
import com.team.cops_and_robbers.report.repository.AdminReportQueryRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class AdminAllReportService {

    private final AdminReportQueryRepository adminReportQueryRepository;
    private final UserRepository userRepository;

    public AdminAllReportPageResult getAllReportList(AdminAllReportListCommand command) {
        String status = command.status() != null ? command.status().name() : null;
        String source = command.source() != null ? command.source().name() : null;

        List<AdminAllReportRow> rows = command.sortDirection() == SortDirection.ASC
                ? adminReportQueryRepository.findAllAsc(status, source, command.size(), command.offset())
                : adminReportQueryRepository.findAllDesc(status, source, command.size(), command.offset());
        long totalElements = adminReportQueryRepository.countAll(status, source);

        Set<Long> userIds = rows.stream()
                .flatMap(row -> Stream.of(row.getReporterUserId(), row.getReportedUserId()))
                .collect(Collectors.toSet());

        Map<Long, String> nicknameByUserId = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        List<AdminAllReportResult> content = rows.stream()
                .map(row -> AdminAllReportResult.of(
                        row,
                        nicknameByUserId.get(row.getReporterUserId()),
                        nicknameByUserId.get(row.getReportedUserId())
                ))
                .toList();

        return AdminAllReportPageResult.of(content, totalElements, command.page(), command.size());
    }
}
