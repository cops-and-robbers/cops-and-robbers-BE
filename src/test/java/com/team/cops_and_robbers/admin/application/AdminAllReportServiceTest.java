package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.report.AdminAllReportListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.report.AdminAllReportPageResult;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.report.domain.ReportSource;
import com.team.cops_and_robbers.report.domain.ReportStatus;
import com.team.cops_and_robbers.report.repository.AdminAllReportRow;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class AdminAllReportServiceTest extends ServiceUnitTest {

    @InjectMocks
    private AdminAllReportService adminAllReportService;

    private User reporter;
    private User reported;

    @BeforeEach
    void setUp() {
        reporter = UserFixture.USER("reporter");
        setId(reporter, 1L);

        reported = UserFixture.USER("reported");
        setId(reported, 2L);
    }

    private AdminAllReportRow communityChatRow() {
        AdminAllReportRow row = mock(AdminAllReportRow.class);
        given(row.getId()).willReturn(400L);
        given(row.getSource()).willReturn("COMMUNITY_CHAT");
        given(row.getReporterUserId()).willReturn(1L);
        given(row.getReportedUserId()).willReturn(2L);
        given(row.getStatus()).willReturn("PENDING");
        given(row.getCreatedAt()).willReturn(LocalDateTime.now());
        given(row.getContent()).willReturn("욕설 메시지");
        return row;
    }

    @Nested
    @DisplayName("통합 신고 목록 조회")
    class GetAllReportList {

        @Test
        void 필터_없이_조회하면_기본_정렬로_전체_신고를_반환한다() {
            // given
            AdminAllReportListCommand command = new AdminAllReportListCommand(0, 10, null, null, SortDirection.DESC);
            AdminAllReportRow row = communityChatRow();
            given(adminReportQueryRepository.findAllDesc(isNull(), isNull(), anyInt(), anyLong()))
                    .willReturn(List.of(row));
            given(adminReportQueryRepository.countAll(isNull(), isNull())).willReturn(1L);
            given(userRepository.findAllById(any())).willReturn(List.of(reporter, reported));

            // when
            AdminAllReportPageResult result = adminAllReportService.getAllReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(1L);
                softly.assertThat(result.content()).hasSize(1);
                softly.assertThat(result.content().get(0).source()).isEqualTo(ReportSource.COMMUNITY_CHAT);
                softly.assertThat(result.content().get(0).content()).isEqualTo("욕설 메시지");
                softly.assertThat(result.content().get(0).reporterNickname()).isEqualTo("reporter");
                softly.assertThat(result.content().get(0).reportedNickname()).isEqualTo("reported");
            });
        }

        @Test
        void status와_source를_동시에_걸면_둘_다_리포지토리로_전달된다() {
            // given
            AdminAllReportListCommand command = new AdminAllReportListCommand(
                    0, 10, ReportStatus.PENDING, ReportSource.COMMUNITY_CHAT, SortDirection.DESC);
            AdminAllReportRow row = communityChatRow();
            given(adminReportQueryRepository.findAllDesc("PENDING", "COMMUNITY_CHAT", 10, 0L))
                    .willReturn(List.of(row));
            given(adminReportQueryRepository.countAll("PENDING", "COMMUNITY_CHAT")).willReturn(1L);
            given(userRepository.findAllById(any())).willReturn(List.of(reporter, reported));

            // when
            adminAllReportService.getAllReportList(command);

            // then
            then(adminReportQueryRepository).should().findAllDesc("PENDING", "COMMUNITY_CHAT", 10, 0L);
            then(adminReportQueryRepository).should().countAll("PENDING", "COMMUNITY_CHAT");
        }

        @Test
        void 정렬_방향이_ASC면_findAllAsc를_호출한다() {
            // given
            AdminAllReportListCommand command = new AdminAllReportListCommand(0, 10, null, null, SortDirection.ASC);
            AdminAllReportRow row = communityChatRow();
            given(adminReportQueryRepository.findAllAsc(isNull(), isNull(), anyInt(), anyLong()))
                    .willReturn(List.of(row));
            given(adminReportQueryRepository.countAll(isNull(), isNull())).willReturn(1L);
            given(userRepository.findAllById(any())).willReturn(List.of(reporter, reported));

            // when
            adminAllReportService.getAllReportList(command);

            // then
            then(adminReportQueryRepository).should().findAllAsc(isNull(), isNull(), anyInt(), anyLong());
            then(adminReportQueryRepository).should(never()).findAllDesc(any(), any(), anyInt(), anyLong());
        }

        @Test
        void 탈퇴한_유저의_닉네임은_알수없음으로_반환된다() {
            // given
            AdminAllReportListCommand command = new AdminAllReportListCommand(0, 10, null, null, SortDirection.DESC);
            AdminAllReportRow row = communityChatRow();
            given(adminReportQueryRepository.findAllDesc(isNull(), isNull(), anyInt(), anyLong()))
                    .willReturn(List.of(row));
            given(adminReportQueryRepository.countAll(isNull(), isNull())).willReturn(1L);
            given(userRepository.findAllById(any())).willReturn(List.of()); // 탈퇴

            // when
            AdminAllReportPageResult result = adminAllReportService.getAllReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.content().get(0).reporterNickname()).isEqualTo("알수없음");
                softly.assertThat(result.content().get(0).reportedNickname()).isEqualTo("알수없음");
            });
        }

        @Test
        void 결과가_없으면_빈_페이지를_반환한다() {
            // given
            AdminAllReportListCommand command = new AdminAllReportListCommand(0, 10, null, null, SortDirection.DESC);
            given(adminReportQueryRepository.findAllDesc(isNull(), isNull(), anyInt(), anyLong()))
                    .willReturn(List.of());
            given(adminReportQueryRepository.countAll(isNull(), isNull())).willReturn(0L);

            // when
            AdminAllReportPageResult result = adminAllReportService.getAllReportList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isZero();
                softly.assertThat(result.content()).isEmpty();
            });
        }
    }
}
