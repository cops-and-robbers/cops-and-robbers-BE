package com.team.cops_and_robbers.report.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.chat.common.repository.CommunityChatMessageRepository;
import com.team.cops_and_robbers.community.post.domain.CommunityPost;
import com.team.cops_and_robbers.community.post.repository.CommunityPostRepository;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.report.application.dto.command.CommunityChatReportCommand;
import com.team.cops_and_robbers.report.application.dto.command.CommunityPostReportCommand;
import com.team.cops_and_robbers.report.application.dto.command.ReportCommand;
import com.team.cops_and_robbers.report.domain.ChatReport;
import com.team.cops_and_robbers.report.domain.CommunityChatReport;
import com.team.cops_and_robbers.report.domain.CommunityPostReport;
import com.team.cops_and_robbers.report.domain.ReportType;
import com.team.cops_and_robbers.report.exception.ReportException;
import com.team.cops_and_robbers.report.repository.CommunityChatReportRepository;
import com.team.cops_and_robbers.report.repository.CommunityPostReportRepository;
import com.team.cops_and_robbers.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final CommunityPostReportRepository communityPostReportRepository;
    private final CommunityChatReportRepository communityChatReportRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;

    @Transactional
    public void reportChat(ReportCommand command) {
        Long reportedUserId = validateReport(command);
        validateEtcReason(command.reportType(), command.etcReason());
        try {
            reportRepository.save(ChatReport.create(command.gameId(), command.reporterUserId(), reportedUserId,
                    command.messageContent(), command.reportType(), command.etcReason()));
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(ReportException.DUPLICATE_REPORT);
        }
    }

    @Transactional
    public void reportCommunityPost(CommunityPostReportCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateNotSelfReport(command.reporterUserId(), post.getWriterId());
        validateEtcReason(command.reportType(), command.etcReason());
        try {
            communityPostReportRepository.save(CommunityPostReport.create(command.postId(), post.getTitle(), post.getContent(),
                    command.reporterUserId(), post.getWriterId(), command.reportType(), command.etcReason()));
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(ReportException.DUPLICATE_REPORT);
        }
    }

    @Transactional
    public void reportCommunityChat(CommunityChatReportCommand command) {
        CommunityChatMessage message = communityChatMessageRepository.getById(command.chatMessageId());
        validateNotSelfReport(command.reporterUserId(), message.getSenderId());
        validateEtcReason(command.reportType(), command.etcReason());
        try {
            communityChatReportRepository.save(CommunityChatReport.create(command.chatMessageId(), command.reporterUserId(),
                    message.getSenderId(), message.getMessage(), command.reportType(), command.etcReason()));
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(ReportException.DUPLICATE_REPORT);
        }
    }

    private void validateNotSelfReport(Long reporterUserId, Long reportedUserId) {
        if (reporterUserId.equals(reportedUserId)) {
            throw new ApplicationException(ReportException.SELF_REPORT);
        }
    }

    private void validateEtcReason(ReportType reportType, String etcReason) {
        if (reportType == ReportType.ETC && (etcReason == null || etcReason.isBlank())) {
            throw new ApplicationException(ReportException.ETC_REASON_REQUIRED);
        }
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

        validateNotSelfReport(command.reporterUserId(), reportedUserId);

        return reportedUserId;
    }
}
