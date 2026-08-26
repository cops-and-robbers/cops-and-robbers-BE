package com.team.cops_and_robbers.report.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.report.application.dto.command.CommunityChatReportCommand;
import com.team.cops_and_robbers.report.application.dto.command.CommunityPostReportCommand;
import com.team.cops_and_robbers.report.application.dto.command.ReportCommand;
import com.team.cops_and_robbers.report.domain.ReportType;
import com.team.cops_and_robbers.report.exception.ReportException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.GUEST_PARTICIPANT;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class ReportServiceTest extends ServiceUnitTest {

    @InjectMocks
    private ReportService reportService;

    private User reporter;
    private User reported;
    private Game game;
    private GameParticipant reporterParticipant;
    private GameParticipant reportedParticipant;

    @BeforeEach
    void setUp() {
        reporter = USER();
        reported = USER();
        game = IN_PROGRESS_GAME();
        reporterParticipant = GUEST_PARTICIPANT(game, reporter);
        reportedParticipant = GUEST_PARTICIPANT(game, reported);

        setId(reporter, 1L);
        setId(reported, 2L);
        setId(game, 10L);
        setId(reporterParticipant, 98L);
        setId(reportedParticipant, 99L);
    }

    @Nested
    @DisplayName("채팅 신고")
    class ChatReport {

        @Test
        void 신고에_성공하면_저장된다() {
            given(gameParticipantRepository.findByGameIdAndUserIdWithGame(10L, 1L)).willReturn(Optional.of(reporterParticipant));
            given(gameParticipantRepository.findByIdAndGameId(99L, 10L)).willReturn(Optional.of(reportedParticipant));

            reportService.reportChat(new ReportCommand(10L, 1L, 99L, "나쁜 말", ReportType.VERBAL_ABUSE, null));

            then(reportRepository).should().save(any());
        }

        @Test
        void 신고자가_해당_게임_참가자가_아니면_예외가_발생한다() {
            given(gameParticipantRepository.findByGameIdAndUserIdWithGame(10L, 1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.reportChat(new ReportCommand(10L, 1L, 99L, "나쁜 말", ReportType.VERBAL_ABUSE, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameParticipantException.PARTICIPANT_NOT_FOUND.getDetail());
        }

        @Test
        void 게임이_진행중이_아니면_예외가_발생한다() {
            Game waitingGame = WAITING_GAME();
            setId(waitingGame, 10L);
            GameParticipant waitingParticipant = GUEST_PARTICIPANT(waitingGame, reporter);
            given(gameParticipantRepository.findByGameIdAndUserIdWithGame(10L, 1L)).willReturn(Optional.of(waitingParticipant));

            assertThatThrownBy(() -> reportService.reportChat(new ReportCommand(10L, 1L, 99L, "나쁜 말", ReportType.VERBAL_ABUSE, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(GameException.GAME_NOT_IN_PROGRESS.getDetail());
        }

        @Test
        void 해당_게임에_없는_참가자를_신고하면_예외가_발생한다() {
            given(gameParticipantRepository.findByGameIdAndUserIdWithGame(10L, 1L)).willReturn(Optional.of(reporterParticipant));
            given(gameParticipantRepository.findByIdAndGameId(99L, 10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.reportChat(new ReportCommand(10L, 1L, 99L, "나쁜 말", ReportType.VERBAL_ABUSE, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ReportException.REPORT_TARGET_NOT_FOUND.getDetail());
        }

        @Test
        void 본인을_신고하면_예외가_발생한다() {
            given(gameParticipantRepository.findByGameIdAndUserIdWithGame(10L, 1L)).willReturn(Optional.of(reporterParticipant));
            given(gameParticipantRepository.findByIdAndGameId(98L, 10L)).willReturn(Optional.of(reporterParticipant));

            assertThatThrownBy(() -> reportService.reportChat(new ReportCommand(10L, 1L, 98L, "나쁜 말", ReportType.VERBAL_ABUSE, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ReportException.SELF_REPORT.getDetail());
        }

        @Test
        void 이미_신고한_유저면_예외가_발생한다() {
            given(gameParticipantRepository.findByGameIdAndUserIdWithGame(10L, 1L)).willReturn(Optional.of(reporterParticipant));
            given(gameParticipantRepository.findByIdAndGameId(99L, 10L)).willReturn(Optional.of(reportedParticipant));
            given(reportRepository.save(any())).willThrow(DataIntegrityViolationException.class);

            assertThatThrownBy(() -> reportService.reportChat(new ReportCommand(10L, 1L, 99L, "나쁜 말", ReportType.VERBAL_ABUSE, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ReportException.DUPLICATE_REPORT.getDetail());
        }

        @Test
        void ETC_신고_유형에_사유가_없으면_예외가_발생한다() {
            given(gameParticipantRepository.findByGameIdAndUserIdWithGame(10L, 1L)).willReturn(Optional.of(reporterParticipant));
            given(gameParticipantRepository.findByIdAndGameId(99L, 10L)).willReturn(Optional.of(reportedParticipant));

            assertThatThrownBy(() -> reportService.reportChat(new ReportCommand(10L, 1L, 99L, "나쁜 말", ReportType.ETC, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ReportException.ETC_REASON_REQUIRED.getDetail());
        }

        @Test
        void ETC_신고_유형에_사유가_있으면_저장된다() {
            given(gameParticipantRepository.findByGameIdAndUserIdWithGame(10L, 1L)).willReturn(Optional.of(reporterParticipant));
            given(gameParticipantRepository.findByIdAndGameId(99L, 10L)).willReturn(Optional.of(reportedParticipant));

            reportService.reportChat(new ReportCommand(10L, 1L, 99L, "나쁜 말", ReportType.ETC, "기타 사유입니다."));

            then(reportRepository).should().save(any());
        }
    }

    @Nested
    @DisplayName("커뮤니티 모집글 신고")
    class CommunityPostReport {

        @Test
        void 신고에_성공하면_저장된다() {
            CommunityPost post = POST(2L);
            setId(post, 5L);
            given(communityPostRepository.getByPostId(5L)).willReturn(post);

            reportService.reportCommunityPost(new CommunityPostReportCommand(5L, 1L, ReportType.SPAM, null));

            then(communityPostReportRepository).should().save(any());
        }

        @Test
        void 존재하지_않는_게시글을_신고하면_예외가_발생한다() {
            given(communityPostRepository.getByPostId(5L))
                    .willThrow(new ApplicationException(CommunityPostException.POST_NOT_FOUND));

            assertThatThrownBy(() -> reportService.reportCommunityPost(new CommunityPostReportCommand(5L, 1L, ReportType.SPAM, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityPostException.POST_NOT_FOUND.getDetail());
        }

        @Test
        void 본인_게시글을_신고하면_예외가_발생한다() {
            CommunityPost post = POST(1L);
            setId(post, 5L);
            given(communityPostRepository.getByPostId(5L)).willReturn(post);

            assertThatThrownBy(() -> reportService.reportCommunityPost(new CommunityPostReportCommand(5L, 1L, ReportType.SPAM, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ReportException.SELF_REPORT.getDetail());
        }

        @Test
        void 이미_신고한_게시글이면_예외가_발생한다() {
            CommunityPost post = POST(2L);
            setId(post, 5L);
            given(communityPostRepository.getByPostId(5L)).willReturn(post);
            given(communityPostReportRepository.save(any())).willThrow(DataIntegrityViolationException.class);

            assertThatThrownBy(() -> reportService.reportCommunityPost(new CommunityPostReportCommand(5L, 1L, ReportType.SPAM, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ReportException.DUPLICATE_REPORT.getDetail());
        }

        @Test
        void ETC_신고_유형에_사유가_없으면_예외가_발생한다() {
            CommunityPost post = POST(2L);
            setId(post, 5L);
            given(communityPostRepository.getByPostId(5L)).willReturn(post);

            assertThatThrownBy(() -> reportService.reportCommunityPost(new CommunityPostReportCommand(5L, 1L, ReportType.ETC, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ReportException.ETC_REASON_REQUIRED.getDetail());
        }

        @Test
        void ETC_신고_유형에_사유가_있으면_저장된다() {
            CommunityPost post = POST(2L);
            setId(post, 5L);
            given(communityPostRepository.getByPostId(5L)).willReturn(post);

            reportService.reportCommunityPost(new CommunityPostReportCommand(5L, 1L, ReportType.ETC, "기타 사유입니다."));

            then(communityPostReportRepository).should().save(any());
        }
    }

    @Nested
    @DisplayName("커뮤니티 채팅 신고")
    class CommunityChatReport {

        private CommunityChatMessage message(Long senderId) {
            CommunityChatMessage message = CommunityChatMessage.createMessage(
                    "key-1", 5L, senderId, "닉네임", "나쁜 말", CommunityChatMessageType.TEXT);
            ReflectionTestUtils.setField(message, "id", 42L);
            return message;
        }

        @Test
        void 신고에_성공하면_저장된다() {
            given(communityChatMessageRepository.getById(42L)).willReturn(message(2L));

            reportService.reportCommunityChat(new CommunityChatReportCommand(42L, 1L, ReportType.VERBAL_ABUSE, null));

            then(communityChatReportRepository).should().save(any());
        }

        @Test
        void 존재하지_않는_메시지를_신고하면_예외가_발생한다() {
            given(communityChatMessageRepository.getById(42L))
                    .willThrow(new ApplicationException(CommunityChatException.CHAT_MESSAGE_NOT_FOUND));

            assertThatThrownBy(() -> reportService.reportCommunityChat(new CommunityChatReportCommand(42L, 1L, ReportType.VERBAL_ABUSE, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityChatException.CHAT_MESSAGE_NOT_FOUND.getDetail());
        }

        @Test
        void 본인_메시지를_신고하면_예외가_발생한다() {
            given(communityChatMessageRepository.getById(42L)).willReturn(message(1L));

            assertThatThrownBy(() -> reportService.reportCommunityChat(new CommunityChatReportCommand(42L, 1L, ReportType.VERBAL_ABUSE, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ReportException.SELF_REPORT.getDetail());
        }

        @Test
        void 이미_신고한_메시지면_예외가_발생한다() {
            given(communityChatMessageRepository.getById(42L)).willReturn(message(2L));
            given(communityChatReportRepository.save(any())).willThrow(DataIntegrityViolationException.class);

            assertThatThrownBy(() -> reportService.reportCommunityChat(new CommunityChatReportCommand(42L, 1L, ReportType.VERBAL_ABUSE, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ReportException.DUPLICATE_REPORT.getDetail());
        }

        @Test
        void ETC_신고_유형에_사유가_없으면_예외가_발생한다() {
            given(communityChatMessageRepository.getById(42L)).willReturn(message(2L));

            assertThatThrownBy(() -> reportService.reportCommunityChat(new CommunityChatReportCommand(42L, 1L, ReportType.ETC, null)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ReportException.ETC_REASON_REQUIRED.getDetail());
        }

        @Test
        void ETC_신고_유형에_사유가_있으면_저장된다() {
            given(communityChatMessageRepository.getById(42L)).willReturn(message(2L));

            reportService.reportCommunityChat(new CommunityChatReportCommand(42L, 1L, ReportType.ETC, "기타 사유입니다."));

            then(communityChatReportRepository).should().save(any());
        }
    }
}
