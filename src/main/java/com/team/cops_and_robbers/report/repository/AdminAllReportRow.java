package com.team.cops_and_robbers.report.repository;

import java.time.LocalDateTime;

public interface AdminAllReportRow {

    Long getId();

    String getSource();

    Long getReporterUserId();

    Long getReportedUserId();

    String getStatus();

    LocalDateTime getCreatedAt();

    String getContent();
}
