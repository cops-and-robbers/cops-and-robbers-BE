package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.result.dashboard.AdminDashboardResult;
import com.team.cops_and_robbers.admin.application.dto.result.dashboard.EndReasonDistributionResult;
import com.team.cops_and_robbers.admin.application.dto.result.dashboard.WinRateByTeamResult;
import com.team.cops_and_robbers.bug.domain.BugReportStatus;
import com.team.cops_and_robbers.bug.repository.BugReportRepository;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.repository.GameResultRepository;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import com.team.cops_and_robbers.report.repository.ReportRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int WEEKLY_DAYS = 6;

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final BugReportRepository bugReportRepository;
    private final GameResultRepository gameResultRepository;

    public AdminDashboardResult getDashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(WEEKLY_DAYS).atStartOfDay();

        List<EndReasonDistributionResult> endReasonDistribution = gameResultRepository
                .countGroupByEndReason().stream()
                .map(EndReasonDistributionResult::from)
                .toList();

        Double averageDuration = gameResultRepository.averageDurationSeconds();

        WinRateByTeamResult winRateByTeam = calculateWinRate(
                gameResultRepository.countByWinnerTeam(Team.POLICE),
                gameResultRepository.countByWinnerTeam(Team.ROBBER)
        );

        return new AdminDashboardResult(
                gameRepository.countByCreatedAtAfter(todayStart),
                gameRepository.countByCreatedAtAfter(weekStart),
                gameRepository.countByStatus(GameStatus.IN_PROGRESS),
                userRepository.count(),
                userRepository.countByCreatedAtAfter(todayStart),
                reportRepository.countByStatus(ReportStatus.PENDING),
                bugReportRepository.countByStatus(BugReportStatus.PENDING),
                endReasonDistribution,
                averageDuration != null ? averageDuration : 0.0,
                winRateByTeam
        );
    }

    private WinRateByTeamResult calculateWinRate(long policeWins, long robberWins) {
        long total = policeWins + robberWins;

        if (total == 0) {
            return new WinRateByTeamResult(0.0, 0.0);
        }

        return new WinRateByTeamResult(
                (double) policeWins / total * 100,
                (double) robberWins / total * 100
        );
    }
}
