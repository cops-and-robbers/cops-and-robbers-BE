package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminUpdateReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminCommunityChatReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminCommunityChatReportResult;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fixture.CommunityChatReportFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.report.domain.CommunityChatReport;
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

class AdminCommunityChatReportServiceTest extends ServiceUnitTest {

    @InjectMocks
    private AdminCommunityChatReportService adminCommunityChatReportService;

    private User reporter;
    private User reported;
    private CommunityChatReport communityChatReport;

    @BeforeEach
    void setUp() {
        reporter = UserFixture.USER("reporter");
        setId(reporter, 1L);

        reported = UserFixture.USER("reported");
        setId(reported, 2L);

        communityChatReport = CommunityChatReportFixture.VERBAL_ABUSE_REPORT(42L, 1L, 2L);
        setId(communityChatReport, 300L);
        ReflectionTestUtils.setField(communityChatReport, "createdAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("커뮤니티 채팅 신고 목록 조회")
    class GetCommunityChatReportList {

        @Test
        void 신고_목록을_조회하면_메시지_스냅샷을_포함한_페이지를_반환한다() {
            // given
            AdminReportListCommand command = new AdminReportListCommand(0, 10, null, SortDirection.DESC);
            Page<CommunityChatReport> reportPage = new PageImpl<>(List.of(communityChatReport), PageRequest.of(0, 10), 1);
            given(communityChatReportRepository.findAllForAdmin(isNull(), any())).willReturn(reportPage);
            given(userRepository.findAllById(any())).willReturn(List.of(reporter, reported));

            // when
            AdminCommunityChatReportPageResult result = adminCommunityChatReportService.getCommunityChatReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(1);
                softly.assertThat(result.content().get(0).messageContent()).isEqualTo("욕설 메시지");
                softly.assertThat(result.content().get(0).reporterNickname()).isEqualTo("reporter");
                softly.assertThat(result.content().get(0).reportedNickname()).isEqualTo("reported");
            });
        }

        @Test
        void 탈퇴한_유저의_닉네임은_탈퇴한_사용자로_반환된다() {
            // given
            AdminReportListCommand command = new AdminReportListCommand(0, 10, null, SortDirection.DESC);
            Page<CommunityChatReport> reportPage = new PageImpl<>(List.of(communityChatReport), PageRequest.of(0, 10), 1);
            given(communityChatReportRepository.findAllForAdmin(isNull(), any())).willReturn(reportPage);
            given(userRepository.findAllById(any())).willReturn(List.of());

            // when
            AdminCommunityChatReportPageResult result = adminCommunityChatReportService.getCommunityChatReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.content().get(0).reporterNickname()).isEqualTo("탈퇴한 사용자");
                softly.assertThat(result.content().get(0).reportedNickname()).isEqualTo("탈퇴한 사용자");
            });
        }
    }

    @Nested
    @DisplayName("커뮤니티 채팅 신고 상태 변경")
    class UpdateCommunityChatReportStatus {

        @Test
        void 신고_상태를_변경하면_변경된_신고_정보를_반환한다() {
            // given
            AdminUpdateReportStatusCommand command = new AdminUpdateReportStatusCommand(
                    300L, ReportStatus.DISMISSED, "근거 부족");
            given(communityChatReportRepository.getById(300L)).willReturn(communityChatReport);
            given(userRepository.findAllById(any())).willReturn(List.of(reporter, reported));

            // when
            AdminCommunityChatReportResult result = adminCommunityChatReportService.updateCommunityChatReportStatus(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.status()).isEqualTo(ReportStatus.DISMISSED);
                softly.assertThat(result.adminMemo()).isEqualTo("근거 부족");
                softly.assertThat(result.reporterNickname()).isEqualTo("reporter");
            });
        }
    }
}
