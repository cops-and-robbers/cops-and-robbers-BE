package com.team.cops_and_robbers.report.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.report.application.dto.command.ReportCommand;
import com.team.cops_and_robbers.report.domain.ChatReport;
import com.team.cops_and_robbers.report.exception.ReportException;
import com.team.cops_and_robbers.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final GameParticipantRepository gameParticipantRepository;

    @Transactional
    public void reportChat(ReportCommand command) {
        Long reportedUserId = validateReport(command);
        reportRepository.save(ChatReport.create(command.gameId(), command.reporterUserId(), reportedUserId, command.messageContent()));
    }

    private Long validateReport(ReportCommand command) {
        GameParticipant reporter = gameParticipantRepository.findByGameIdAndUserIdWithGame(command.gameId(), command.reporterUserId())
                .orElseThrow(() -> new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));

        if (!reporter.getGame().isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_IN_PROGRESS);
        }

        GameParticipant reported = gameParticipantRepository.findByIdAndGameId(command.reportedParticipantId(), command.gameId())
                .orElseThrow(() -> new ApplicationException(ReportException.REPORT_TARGET_NOT_FOUND));
        Long reportedUserId = reported.getUser().getId();

        if (command.reporterUserId().equals(reportedUserId)) {
            throw new ApplicationException(ReportException.SELF_REPORT);
        }

        if (reportRepository.existsByReporterUserIdAndReportedUserIdAndGameId(command.reporterUserId(), reportedUserId, command.gameId())) {
            throw new ApplicationException(ReportException.DUPLICATE_REPORT);
        }

        return reportedUserId;
    }
}

