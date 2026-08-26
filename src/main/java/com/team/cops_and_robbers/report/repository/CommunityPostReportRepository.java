package com.team.cops_and_robbers.report.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.report.domain.CommunityPostReport;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import com.team.cops_and_robbers.report.exception.ReportException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostReportRepository extends JpaRepository<CommunityPostReport, Long> {

    @Query("""
            select r from CommunityPostReport r
            where (:status is null or r.status = :status)
            """)
    Page<CommunityPostReport> findAllForAdmin(@Param("status") ReportStatus status, Pageable pageable);

    default CommunityPostReport getById(Long id) {
        return findById(id)
                .orElseThrow(() -> new ApplicationException(ReportException.REPORT_NOT_FOUND));
    }
}
