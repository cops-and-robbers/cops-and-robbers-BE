package com.team.cops_and_robbers.report.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.report.domain.ChatReport;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import com.team.cops_and_robbers.report.exception.ReportException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<ChatReport, Long> {

    @Query("""
            select r from ChatReport r
            where (:status is null or r.status = :status)
            """)
    Page<ChatReport> findAllForAdmin(@Param("status") ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);

    default ChatReport getById(Long id) {
        return findById(id)
                .orElseThrow(() -> new ApplicationException(ReportException.REPORT_NOT_FOUND));
    }
}
