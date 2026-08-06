package com.team.cops_and_robbers.bug.repository;

import com.team.cops_and_robbers.bug.domain.BugReport;
import com.team.cops_and_robbers.bug.domain.BugReportStatus;
import com.team.cops_and_robbers.bug.exception.BugReportException;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BugReportRepository extends JpaRepository<BugReport, Long> {

    @Query("""
            select b from BugReport b
            where (:status is null or b.status = :status)
            """)
    Page<BugReport> findAllForAdmin(@Param("status") BugReportStatus status, Pageable pageable);

    long countByStatus(BugReportStatus status);

    default BugReport getById(Long id) {
        return findById(id)
                .orElseThrow(() -> new ApplicationException(BugReportException.BUG_REPORT_NOT_FOUND));
    }
}
