package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.bug.AdminBugReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.command.bug.AdminUpdateBugReportStatusCommand;
import com.team.cops_and_robbers.admin.application.dto.result.bug.AdminBugReportPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.bug.AdminBugReportResult;
import com.team.cops_and_robbers.bug.domain.BugReport;
import com.team.cops_and_robbers.bug.domain.BugReportStatus;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fixture.BugReportFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isNull;

class AdminBugReportServiceTest extends ServiceUnitTest {

    @InjectMocks
    private AdminBugReportService adminBugReportService;

    private User user;
    private BugReport bugReport;

    @BeforeEach
    void setUp() {
        user = UserFixture.USER("reporter");
        setId(user, 1L);

        bugReport = BugReportFixture.PENDING_BUG_REPORT(1L);
        setId(bugReport, 100L);
        ReflectionTestUtils.setField(bugReport, "createdAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("버그 제보 목록 조회")
    class GetBugReportList {

        @Test
        void 필터_없이_버그_제보_목록을_조회하면_전체_페이지를_반환한다() {
            // given
            AdminBugReportListCommand command = new AdminBugReportListCommand(0, 10, null, SortDirection.DESC);
            Page<BugReport> bugReportPage = new PageImpl<>(List.of(bugReport), PageRequest.of(0, 10), 1);
            given(bugReportRepository.findAllForAdmin(isNull(), any())).willReturn(bugReportPage);
            given(userRepository.findAllById(any())).willReturn(List.of(user));

            // when
            AdminBugReportPageResult result = adminBugReportService.getBugReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(1);
                softly.assertThat(result.content()).hasSize(1);
                softly.assertThat(result.content().get(0).userNickname()).isEqualTo("reporter");
                softly.assertThat(result.content().get(0).status()).isEqualTo(BugReportStatus.PENDING);
            });
        }

        @Test
        void 상태_필터로_버그_제보_목록을_조회하면_해당_상태만_반환한다() {
            // given
            AdminBugReportListCommand command = new AdminBugReportListCommand(0, 10, BugReportStatus.PENDING, SortDirection.DESC);
            Page<BugReport> bugReportPage = new PageImpl<>(List.of(bugReport), PageRequest.of(0, 10), 1);
            given(bugReportRepository.findAllForAdmin(any(BugReportStatus.class), any())).willReturn(bugReportPage);
            given(userRepository.findAllById(any())).willReturn(List.of(user));

            // when
            AdminBugReportPageResult result = adminBugReportService.getBugReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(1);
                softly.assertThat(result.content().get(0).status()).isEqualTo(BugReportStatus.PENDING);
            });
        }

        @Test
        void 결과가_없으면_빈_페이지를_반환한다() {
            // given
            AdminBugReportListCommand command = new AdminBugReportListCommand(0, 10, BugReportStatus.RESOLVED, SortDirection.DESC);
            Page<BugReport> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(bugReportRepository.findAllForAdmin(any(BugReportStatus.class), any())).willReturn(emptyPage);

            // when
            AdminBugReportPageResult result = adminBugReportService.getBugReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isZero();
                softly.assertThat(result.content()).isEmpty();
            });
        }

        @Test
        void 탈퇴한_유저의_닉네임은_탈퇴한_사용자로_반환된다() {
            // given
            AdminBugReportListCommand command = new AdminBugReportListCommand(0, 10, null, SortDirection.DESC);
            Page<BugReport> bugReportPage = new PageImpl<>(List.of(bugReport), PageRequest.of(0, 10), 1);
            given(bugReportRepository.findAllForAdmin(isNull(), any())).willReturn(bugReportPage);
            given(userRepository.findAllById(any())).willReturn(List.of()); // 탈퇴

            // when
            AdminBugReportPageResult result = adminBugReportService.getBugReportList(command);

            // then
            assertThat(result.content().get(0).userNickname()).isEqualTo("탈퇴한 사용자");
        }
    }

    @Nested
    @DisplayName("버그 제보 상태 변경")
    class UpdateBugReportStatus {

        @Test
        void 버그_제보_상태를_변경하면_변경된_정보를_반환한다() {
            // given
            AdminUpdateBugReportStatusCommand command = new AdminUpdateBugReportStatusCommand(
                    100L, BugReportStatus.RESOLVED, "처리 완료");
            given(bugReportRepository.getById(100L)).willReturn(bugReport);
            given(userRepository.findAllById(any())).willReturn(List.of(user));

            // when
            AdminBugReportResult result = adminBugReportService.updateBugReportStatus(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.status()).isEqualTo(BugReportStatus.RESOLVED);
                softly.assertThat(result.adminMemo()).isEqualTo("처리 완료");
                softly.assertThat(result.userNickname()).isEqualTo("reporter");
            });
        }

        @Test
        void 상태_변경_시_탈퇴한_유저의_닉네임은_탈퇴한_사용자로_반환된다() {
            // given
            AdminUpdateBugReportStatusCommand command = new AdminUpdateBugReportStatusCommand(
                    100L, BugReportStatus.RESOLVED, null);
            given(bugReportRepository.getById(100L)).willReturn(bugReport);
            given(userRepository.findAllById(any())).willReturn(List.of()); // 탈퇴

            // when
            AdminBugReportResult result = adminBugReportService.updateBugReportStatus(command);

            // then
            assertThat(result.userNickname()).isEqualTo("탈퇴한 사용자");
        }
    }
}
