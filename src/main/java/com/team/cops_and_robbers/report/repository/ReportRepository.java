package com.team.cops_and_robbers.report.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.report.domain.ChatReport;
import com.team.cops_and_robbers.report.exception.ReportException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<ChatReport, Long> {

    boolean existsByReporterUserIdAndReportedUserIdAndGameId(
            Long reporterUserId, Long reportedUserId, Long gameId);

    default ChatReport getById(Long id) {
        return findById(id)
                .orElseThrow(() -> new ApplicationException(ReportException.REPORT_NOT_FOUND));
    }
}
