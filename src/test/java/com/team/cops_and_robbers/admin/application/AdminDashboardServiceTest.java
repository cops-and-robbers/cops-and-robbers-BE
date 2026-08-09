package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.result.dashboard.AdminDashboardResult;
import com.team.cops_and_robbers.bug.domain.BugReportStatus;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.repository.EndReasonCountProjection;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;

class AdminDashboardServiceTest extends ServiceUnitTest {

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Nested
    @DisplayName("대시보드 조회")
    class GetDashboard {

        @Test
        void 대시보드_데이터를_정상적으로_반환한다() {
            // given
            given(gameRepository.countByCreatedAtGreaterThanEqual(any())).willReturn(5L, 20L);
            given(gameRepository.countByStatus(GameStatus.IN_PROGRESS)).willReturn(2L);
            given(userRepository.count()).willReturn(100L);
            given(userRepository.countByCreatedAtGreaterThanEqual(any())).willReturn(3L);
            given(reportRepository.countByStatus(ReportStatus.PENDING)).willReturn(7L);
            given(bugReportRepository.countByStatus(BugReportStatus.PENDING)).willReturn(4L);
            given(gameResultRepository.countByWinnerTeam(Team.POLICE)).willReturn(60L);
            given(gameResultRepository.countByWinnerTeam(Team.ROBBER)).willReturn(40L);
            given(gameResultRepository.averageDurationSeconds()).willReturn(350.0);
            given(gameResultRepository.countGroupByEndReason()).willReturn(List.of(
                    projection(GameEndReason.ALL_ARRESTED, 50L),
                    projection(GameEndReason.TIME_OVER, 30L)
            ));

            // when
            AdminDashboardResult result = adminDashboardService.getDashboard();

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.todayGameCount()).isEqualTo(5L);
                softly.assertThat(result.weeklyGameCount()).isEqualTo(20L);
                softly.assertThat(result.inProgressGameCount()).isEqualTo(2L);
                softly.assertThat(result.totalUserCount()).isEqualTo(100L);
                softly.assertThat(result.todayNewUserCount()).isEqualTo(3L);
                softly.assertThat(result.pendingReportCount()).isEqualTo(7L);
                softly.assertThat(result.pendingBugReportCount()).isEqualTo(4L);
                softly.assertThat(result.averageGameDurationSeconds()).isEqualTo(350.0);
                softly.assertThat(result.endReasonDistribution()).hasSize(2);
                softly.assertThat(result.winRateByTeam().policeWinRate()).isEqualTo(60.0);
                softly.assertThat(result.winRateByTeam().robberWinRate()).isEqualTo(40.0);
            });
        }

        @Test
        void 게임_결과가_없으면_평균_게임_시간은_0이고_승률은_모두_0이다() {
            // given
            given(gameRepository.countByCreatedAtGreaterThanEqual(any())).willReturn(0L);
            given(gameRepository.countByStatus(any())).willReturn(0L);
            given(userRepository.count()).willReturn(0L);
            given(userRepository.countByCreatedAtGreaterThanEqual(any())).willReturn(0L);
            given(reportRepository.countByStatus(any())).willReturn(0L);
            given(bugReportRepository.countByStatus(any())).willReturn(0L);
            given(gameResultRepository.countByWinnerTeam(any())).willReturn(0L);
            given(gameResultRepository.averageDurationSeconds()).willReturn(null);
            given(gameResultRepository.countGroupByEndReason()).willReturn(List.of());

            // when
            AdminDashboardResult result = adminDashboardService.getDashboard();

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.averageGameDurationSeconds()).isEqualTo(0.0);
                softly.assertThat(result.winRateByTeam().policeWinRate()).isEqualTo(0.0);
                softly.assertThat(result.winRateByTeam().robberWinRate()).isEqualTo(0.0);
                softly.assertThat(result.endReasonDistribution()).isEmpty();
            });
        }

        @Test
        void 경찰만_이긴_경우_경찰_승률은_100이고_강도_승률은_0이다() {
            // given
            given(gameRepository.countByCreatedAtGreaterThanEqual(any())).willReturn(0L);
            given(gameRepository.countByStatus(any())).willReturn(0L);
            given(userRepository.count()).willReturn(0L);
            given(userRepository.countByCreatedAtGreaterThanEqual(any())).willReturn(0L);
            given(reportRepository.countByStatus(any())).willReturn(0L);
            given(bugReportRepository.countByStatus(any())).willReturn(0L);
            given(gameResultRepository.countByWinnerTeam(Team.POLICE)).willReturn(10L);
            given(gameResultRepository.countByWinnerTeam(Team.ROBBER)).willReturn(0L);
            given(gameResultRepository.averageDurationSeconds()).willReturn(300.0);
            given(gameResultRepository.countGroupByEndReason()).willReturn(List.of());

            // when
            AdminDashboardResult result = adminDashboardService.getDashboard();

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.winRateByTeam().policeWinRate()).isEqualTo(100.0);
                softly.assertThat(result.winRateByTeam().robberWinRate()).isEqualTo(0.0);
            });
        }
    }

    private EndReasonCountProjection projection(GameEndReason endReason, Long count) {
        return new EndReasonCountProjection() {
            @Override
            public GameEndReason getEndReason() {
                return endReason;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
