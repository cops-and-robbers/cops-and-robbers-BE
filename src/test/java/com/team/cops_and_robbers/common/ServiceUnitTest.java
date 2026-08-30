package com.team.cops_and_robbers.common;

import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.bug.repository.BugReportRepository;
import com.team.cops_and_robbers.community.infrastructure.GeocodingClient;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.community.repository.CommunityChatMessageRepository;
import com.team.cops_and_robbers.community.repository.CommunityCommentRepository;
import com.team.cops_and_robbers.community.repository.CommunityNotificationRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostLikeRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostNotificationSettingRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostScrapRepository;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.history.repository.GameResultParticipantRepository;
import com.team.cops_and_robbers.history.repository.GameResultRepository;
import com.team.cops_and_robbers.notice.repository.NoticeRepository;
import com.team.cops_and_robbers.notice.repository.NoticeTranslationRepository;
import com.team.cops_and_robbers.report.repository.AdminReportQueryRepository;
import com.team.cops_and_robbers.report.repository.CommunityChatReportRepository;
import com.team.cops_and_robbers.report.repository.CommunityPostReportRepository;
import com.team.cops_and_robbers.report.repository.ReportRepository;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public abstract class ServiceUnitTest {

    @Mock
    protected GameRepository gameRepository;

    @Mock
    protected GameParticipantRepository gameParticipantRepository;

    @Mock
    protected GameAreaRepository gameAreaRepository;

    @Mock
    protected GameResultRepository gameResultRepository;

    @Mock
    protected GameResultParticipantRepository gameResultParticipantRepository;

    @Mock
    protected UserRepository userRepository;

    @Mock
    protected ReportRepository reportRepository;

    @Mock
    protected CommunityPostReportRepository communityPostReportRepository;

    @Mock
    protected CommunityChatReportRepository communityChatReportRepository;

    @Mock
    protected AdminReportQueryRepository adminReportQueryRepository;

    @Mock
    protected BugReportRepository bugReportRepository;

    @Mock
    protected UserDeviceRepository userDeviceRepository;

    @Mock
    protected NoticeRepository noticeRepository;

    @Mock
    protected NoticeTranslationRepository noticeTranslationRepository;

    @Mock
    protected CommunityPostRepository communityPostRepository;

    @Mock
    protected CommunityChatMemberRepository communityChatMemberRepository;

    @Mock
    protected CommunityChatMessageRepository communityChatMessageRepository;

    @Mock
    protected CommunityCommentRepository communityCommentRepository;

    @Mock
    protected CommunityPostLikeRepository communityPostLikeRepository;

    @Mock
    protected CommunityPostScrapRepository communityPostScrapRepository;

    @Mock
    protected CommunityNotificationRepository communityNotificationRepository;

    @Mock
    protected CommunityPostNotificationSettingRepository communityPostNotificationSettingRepository;

    @Mock
    protected JwtTokenProvider jwtTokenProvider;

    @Mock
    protected GeocodingClient geocodingClient;

    protected static void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
