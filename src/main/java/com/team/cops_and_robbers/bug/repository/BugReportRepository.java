package com.team.cops_and_robbers.bug.repository;

import com.team.cops_and_robbers.bug.domain.BugReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BugReportRepository extends JpaRepository<BugReport, Long> {}
