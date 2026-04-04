package com.team.cops_and_robbers.report.application.dto.command;

import com.team.cops_and_robbers.report.domain.ReportType;
import com.team.cops_and_robbers.report.presentation.dto.request.ReportRequest;

public record ReportCommand(
        Long gameId,
        Long reporterUserId,
        Long reportedParticipantId,
        String messageContent,
        ReportType reportType,
        String etcReason
) {
    public static ReportCommand of(Long reporterUserId, ReportRequest request) {
        return new ReportCommand(
                request.gameId(),
                reporterUserId,
                request.reportedParticipantId(),
                request.messageContent(),
                request.reportType(),
                request.etcReason()
        );
    }
}
