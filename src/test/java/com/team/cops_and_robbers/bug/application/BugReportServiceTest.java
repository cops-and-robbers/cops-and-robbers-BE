package com.team.cops_and_robbers.bug.application;

import com.team.cops_and_robbers.bug.application.dto.command.BugReportCommand;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.infrastructure.discord.DiscordNotifier;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class BugReportServiceTest extends ServiceUnitTest {

    @InjectMocks
    private BugReportService bugReportService;

    @Mock
    private DiscordNotifier discordNotifier;

    @Nested
    @DisplayName("버그 제보 생성")
    class CreateBugReport {

        @Test
        void 버그_제보에_성공하면_저장되고_Discord_알림이_전송된다() {
            User user = USER("정상희");
            setId(user, 1L);
            given(userRepository.getByUserId(1L)).willReturn(user);

            bugReportService.createBugReport(new BugReportCommand(1L, "게임 시작 버튼을 누르면 앱이 꺼져요"));

            then(bugReportRepository).should().save(any());
            then(discordNotifier).should().sendBugReport("게임 시작 버튼을 누르면 앱이 꺼져요", "정상희");
        }
    }
}
