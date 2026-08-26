package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminUpdateReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminCommunityPostReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminCommunityPostReportResult;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fixture.CommunityPostReportFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.report.domain.CommunityPostReport;
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

class AdminCommunityPostReportServiceTest extends ServiceUnitTest {

    @InjectMocks
    private AdminCommunityPostReportService adminCommunityPostReportService;

    private User reporter;
    private User reported;
    private CommunityPostReport communityPostReport;

    @BeforeEach
    void setUp() {
        reporter = UserFixture.USER("reporter");
        setId(reporter, 1L);

        reported = UserFixture.USER("reported");
        setId(reported, 2L);

        communityPostReport = CommunityPostReportFixture.SPAM_REPORT(5L, 1L, 2L);
        setId(communityPostReport, 200L);
        ReflectionTestUtils.setField(communityPostReport, "createdAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("커뮤니티 모집글 신고 목록 조회")
    class GetCommunityPostReportList {

        @Test
        void 신고_목록을_조회하면_게시글_스냅샷을_포함한_페이지를_반환한다() {
            // given
            AdminReportListCommand command = new AdminReportListCommand(0, 10, null, SortDirection.DESC);
            Page<CommunityPostReport> reportPage = new PageImpl<>(List.of(communityPostReport), PageRequest.of(0, 10), 1);
            given(communityPostReportRepository.findAllForAdmin(isNull(), any())).willReturn(reportPage);
            given(userRepository.findAllById(any())).willReturn(List.of(reporter, reported));

            // when
            AdminCommunityPostReportPageResult result = adminCommunityPostReportService.getCommunityPostReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(1);
                softly.assertThat(result.content().get(0).postTitle()).isEqualTo("같이 경찰과 도둑 하실 분!");
                softly.assertThat(result.content().get(0).reporterNickname()).isEqualTo("reporter");
                softly.assertThat(result.content().get(0).reportedNickname()).isEqualTo("reported");
            });
        }

        @Test
        void 탈퇴한_유저의_닉네임은_탈퇴한_사용자로_반환된다() {
            // given
            AdminReportListCommand command = new AdminReportListCommand(0, 10, null, SortDirection.DESC);
            Page<CommunityPostReport> reportPage = new PageImpl<>(List.of(communityPostReport), PageRequest.of(0, 10), 1);
            given(communityPostReportRepository.findAllForAdmin(isNull(), any())).willReturn(reportPage);
            given(userRepository.findAllById(any())).willReturn(List.of());

            // when
            AdminCommunityPostReportPageResult result = adminCommunityPostReportService.getCommunityPostReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.content().get(0).reporterNickname()).isEqualTo("탈퇴한 사용자");
                softly.assertThat(result.content().get(0).reportedNickname()).isEqualTo("탈퇴한 사용자");
            });
        }
    }

    @Nested
    @DisplayName("커뮤니티 모집글 신고 상태 변경")
    class UpdateCommunityPostReportStatus {

        @Test
        void 신고_상태를_변경하면_변경된_신고_정보를_반환한다() {
            // given
            AdminUpdateReportStatusCommand command = new AdminUpdateReportStatusCommand(
                    200L, ReportStatus.RESOLVED, "확인 완료");
            given(communityPostReportRepository.getById(200L)).willReturn(communityPostReport);
            given(userRepository.findAllById(any())).willReturn(List.of(reporter, reported));

            // when
            AdminCommunityPostReportResult result = adminCommunityPostReportService.updateCommunityPostReportStatus(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.status()).isEqualTo(ReportStatus.RESOLVED);
                softly.assertThat(result.adminMemo()).isEqualTo("확인 완료");
                softly.assertThat(result.reporterNickname()).isEqualTo("reporter");
            });
        }
    }
}
