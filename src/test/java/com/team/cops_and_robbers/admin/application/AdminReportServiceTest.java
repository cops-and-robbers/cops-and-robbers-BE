package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminUpdateReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminReportResult;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fixture.ChatReportFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.report.domain.ChatReport;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isNull;

class AdminReportServiceTest extends ServiceUnitTest {

    @InjectMocks
    private AdminReportService adminReportService;

    private User reporter;
    private User reported;
    private ChatReport report;

    @BeforeEach
    void setUp() {
        reporter = UserFixture.USER("reporter");
        setId(reporter, 1L);

        reported = UserFixture.USER("reported");
        setId(reported, 2L);

        report = ChatReportFixture.VERBAL_ABUSE_REPORT(10L, 1L, 2L);
        setId(report, 100L);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("신고 목록 조회")
    class GetReportList {

        @Test
        void 필터_없이_신고_목록을_조회하면_전체_신고_페이지를_반환한다() {
            // given
            AdminReportListCommand command = new AdminReportListCommand(0, 10, null, SortDirection.DESC);
            Page<ChatReport> reportPage = new PageImpl<>(List.of(report), PageRequest.of(0, 10), 1);
            given(reportRepository.findAllForAdmin(isNull(), any())).willReturn(reportPage);
            given(userRepository.findAllById(any())).willReturn(List.of(reporter, reported));

            // when
            AdminReportPageResult result = adminReportService.getReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(1);
                softly.assertThat(result.content()).hasSize(1);
                softly.assertThat(result.content().get(0).reporterNickname()).isEqualTo("reporter");
                softly.assertThat(result.content().get(0).reportedNickname()).isEqualTo("reported");
            });
        }

        @Test
        void 상태_필터로_신고_목록을_조회하면_해당_상태_신고만_반환한다() {
            // given
            AdminReportListCommand command = new AdminReportListCommand(0, 10, ReportStatus.PENDING, SortDirection.DESC);
            Page<ChatReport> reportPage = new PageImpl<>(List.of(report), PageRequest.of(0, 10), 1);
            given(reportRepository.findAllForAdmin(any(ReportStatus.class), any())).willReturn(reportPage);
            given(userRepository.findAllById(any())).willReturn(List.of(reporter, reported));

            // when
            AdminReportPageResult result = adminReportService.getReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(1);
                softly.assertThat(result.content().get(0).status()).isEqualTo(ReportStatus.PENDING);
            });
        }

        @Test
        void 결과가_없으면_빈_페이지를_반환한다() {
            // given
            AdminReportListCommand command = new AdminReportListCommand(0, 10, ReportStatus.RESOLVED, SortDirection.DESC);
            Page<ChatReport> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(reportRepository.findAllForAdmin(any(ReportStatus.class), any())).willReturn(emptyPage);

            // when
            AdminReportPageResult result = adminReportService.getReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isZero();
                softly.assertThat(result.content()).isEmpty();
            });
        }

        @Test
        void 탈퇴한_유저의_닉네임은_탈퇴한_사용자로_반환된다() {
            // given
            AdminReportListCommand command = new AdminReportListCommand(0, 10, null, SortDirection.DESC);
            Page<ChatReport> reportPage = new PageImpl<>(List.of(report), PageRequest.of(0, 10), 1);
            given(reportRepository.findAllForAdmin(isNull(), any())).willReturn(reportPage);
            given(userRepository.findAllById(any())).willReturn(List.of()); // 유저 없음 = 탈퇴

            // when
            AdminReportPageResult result = adminReportService.getReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.content().get(0).reporterNickname()).isEqualTo("탈퇴한 사용자");
                softly.assertThat(result.content().get(0).reportedNickname()).isEqualTo("탈퇴한 사용자");
            });
        }
    }

    @Nested
    @DisplayName("신고 상태 변경")
    class UpdateReportStatus {

        @Test
        void 신고_상태를_변경하면_변경된_신고_정보를_반환한다() {
            // given
            AdminUpdateReportStatusCommand command = new AdminUpdateReportStatusCommand(
                    100L, ReportStatus.RESOLVED, "확인 완료");
            given(reportRepository.getById(100L)).willReturn(report);
            given(userRepository.findAllById(any())).willReturn(List.of(reporter, reported));

            // when
            AdminReportResult result = adminReportService.updateReportStatus(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED);
                softly.assertThat(result.adminMemo()).isEqualTo("확인 완료");
                softly.assertThat(result.reporterNickname()).isEqualTo("reporter");
            });
        }

        @Test
        void 상태_변경_시_탈퇴한_유저의_닉네임은_탈퇴한_사용자로_반환된다() {
            // given
            AdminUpdateReportStatusCommand command = new AdminUpdateReportStatusCommand(
                    100L, ReportStatus.DISMISSED, null);
            given(reportRepository.getById(100L)).willReturn(report);
            given(userRepository.findAllById(any())).willReturn(List.of()); // 탈퇴

            // when
            AdminReportResult result = adminReportService.updateReportStatus(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.reporterNickname()).isEqualTo("탈퇴한 사용자");
                softly.assertThat(result.reportedNickname()).isEqualTo("탈퇴한 사용자");
            });
        }
    }
}
