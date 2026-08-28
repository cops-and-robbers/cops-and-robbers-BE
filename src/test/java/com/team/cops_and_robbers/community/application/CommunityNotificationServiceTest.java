package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityNotificationListCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostNotificationSettingCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityNotificationListResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityNotificationUnreadCountResult;
import com.team.cops_and_robbers.community.domain.CommunityComment;
import com.team.cops_and_robbers.community.domain.CommunityNotification;
import com.team.cops_and_robbers.community.domain.CommunityNotificationType;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.CommunityPostNotificationSetting;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class CommunityNotificationServiceTest extends ServiceUnitTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 27, 12, 0);
    private static final int RETENTION_DAYS = 60;

    private static final Long POST_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;
    private static final Long COMMENTER_ID = 200L;
    private static final Long PARENT_WRITER_ID = 300L;
    private static final Long THIRD_PARTY_ID = 400L;

    @InjectMocks
    private CommunityNotificationService communityNotificationService;

    @Spy
    private Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);

    private User givenUser(LocalDateTime readAt) {
        User user = USER("알림받는사람");
        setId(user, USER_ID);
        if (readAt != null) {
            user.readCommunityNotifications(readAt);
        }
        given(userRepository.getByUserId(USER_ID)).willReturn(user);
        return user;
    }

    private CommunityNotification notification(Long id, LocalDateTime createdAt) {
        CommunityNotification notification = CommunityNotification.createNotification(
                USER_ID, CommunityNotificationType.COMMENT, POST_ID, "같이 하실 분!", "몇 시에 만나나요?");
        setId(notification, id);
        // createdAt은 JPA auditing이 채우므로 테스트에서는 직접 넣는다
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        ReflectionTestUtils.setField(notification, "updatedAt", createdAt);
        return notification;
    }

    private LocalDateTime captureSince() {
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(communityNotificationRepository).should()
                .findPageByCursor(eq(USER_ID), captor.capture(), any(), any(Pageable.class));
        return captor.getValue();
    }

    private CommunityPost givenPost() {
        CommunityPost post = POST(USER_ID);
        setId(post, POST_ID);
        given(communityPostRepository.getByPostId(POST_ID)).willReturn(post);
        return post;
    }

    private CommunityComment comment(Long id, Long parentId, Long writerId) {
        CommunityComment comment = CommunityComment.createComment(POST_ID, parentId, writerId, "몇 시에 만나나요?");
        setId(comment, id);
        return comment;
    }

    private void givenParentComment(Long parentId, Long writerId) {
        givenParentComment(parentId, writerId, true);
    }

    private void givenParentComment(Long parentId, Long writerId, boolean notifyReplies) {
        CommunityComment parent = comment(parentId, null, writerId);
        parent.updateNotifyReplies(notifyReplies);
        given(communityCommentRepository.getByCommentId(parentId)).willReturn(parent);
    }

    private void givenSettings(CommunityPostNotificationSetting... settings) {
        given(communityPostNotificationSettingRepository.findAllByCommunityPostId(POST_ID))
                .willReturn(List.of(settings));
    }

    private CommunityPostNotificationSetting setting(Long userId, boolean notifyComments, boolean notifyReplies) {
        return CommunityPostNotificationSetting.createSetting(userId, POST_ID, notifyComments, notifyReplies);
    }

    private List<Long> createRecipients(CommunityComment comment) {
        return communityNotificationService.createNotifications(comment).recipients();
    }

    @SuppressWarnings("unchecked")
    private List<CommunityNotification> captureSaved() {
        ArgumentCaptor<List<CommunityNotification>> captor = ArgumentCaptor.forClass(List.class);
        then(communityNotificationRepository).should().saveAll(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("알림함 목록 조회")
    class GetNotifications {

        @Test
        void 요청_크기보다_한_건_더_조회해_다음_페이지_여부를_판단하고_초과분은_걷어낸다() {
            givenUser(null);
            given(communityNotificationRepository.findPageByCursor(anyLong(), any(), any(), any(Pageable.class)))
                    .willReturn(List.of(
                            notification(30L, NOW),
                            notification(29L, NOW),
                            notification(28L, NOW)
                    ));

            CommunityNotificationListResult result = communityNotificationService.getNotifications(
                    CommunityNotificationListCommand.of(USER_ID, null, 2));

            assertSoftly(softly -> {
                softly.assertThat(result.content()).hasSize(2);
                softly.assertThat(result.hasNext()).isTrue();
                softly.assertThat(result.nextCursor()).isEqualTo(29L);
            });
        }

        @Test
        void 마지막_페이지면_다음_커서가_없다() {
            givenUser(null);
            given(communityNotificationRepository.findPageByCursor(anyLong(), any(), any(), any(Pageable.class)))
                    .willReturn(List.of(notification(30L, NOW)));

            CommunityNotificationListResult result = communityNotificationService.getNotifications(
                    CommunityNotificationListCommand.of(USER_ID, null, 2));

            assertSoftly(softly -> {
                softly.assertThat(result.content()).hasSize(1);
                softly.assertThat(result.hasNext()).isFalse();
                softly.assertThat(result.nextCursor()).isNull();
            });
        }

        @Test
        void 읽음_커서보다_나중에_생긴_알림만_안_읽음으로_표시한다() {
            LocalDateTime readAt = NOW.minusHours(1);
            givenUser(readAt);
            given(communityNotificationRepository.findPageByCursor(anyLong(), any(), any(), any(Pageable.class)))
                    .willReturn(List.of(
                            notification(30L, readAt.plusMinutes(10)),
                            notification(29L, readAt.minusMinutes(10))
                    ));

            CommunityNotificationListResult result = communityNotificationService.getNotifications(
                    CommunityNotificationListCommand.of(USER_ID, null, 10));

            assertSoftly(softly -> {
                softly.assertThat(result.content().get(0).read()).isFalse();
                softly.assertThat(result.content().get(1).read()).isTrue();
            });
        }

        @Test
        void 읽음_커서가_없으면_전부_안_읽음이다() {
            givenUser(null);
            given(communityNotificationRepository.findPageByCursor(anyLong(), any(), any(), any(Pageable.class)))
                    .willReturn(List.of(notification(30L, NOW.minusDays(30))));

            CommunityNotificationListResult result = communityNotificationService.getNotifications(
                    CommunityNotificationListCommand.of(USER_ID, null, 10));

            assertThat(result.content().getFirst().read()).isFalse();
        }

        @Test
        void 보관_기간을_넘긴_알림은_조회_조건에서_잘린다() {
            givenUser(null);
            given(communityNotificationRepository.findPageByCursor(anyLong(), any(), any(), any(Pageable.class)))
                    .willReturn(List.of());

            communityNotificationService.getNotifications(
                    CommunityNotificationListCommand.of(USER_ID, null, 10));

            assertThat(captureSince()).isEqualTo(NOW.minusDays(RETENTION_DAYS));
        }
    }

    @Nested
    @DisplayName("안 읽은 알림 개수")
    class GetUnreadCount {

        @Test
        void 유저의_읽음_커서와_보관_기간을_기준으로_센다() {
            LocalDateTime readAt = NOW.minusDays(1);
            givenUser(readAt);
            given(communityNotificationRepository.countUnread(
                    USER_ID, NOW.minusDays(RETENTION_DAYS), readAt)).willReturn(3L);

            CommunityNotificationUnreadCountResult result =
                    communityNotificationService.getUnreadCount(USER_ID);

            assertThat(result.unreadCount()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("읽음 처리")
    class ReadNotifications {

        @Test
        void 유저의_읽음_커서를_현재_시각으로_갱신한다() {
            User user = givenUser(null);

            communityNotificationService.readNotifications(USER_ID);

            assertThat(user.getCommunityNotificationReadAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("게시글별 알림 설정 변경")
    class UpdateSetting {

        @Test
        void 토글을_처음_건드리면_행을_만든다() {
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(POST(USER_ID));
            given(communityPostNotificationSettingRepository.findByCommunityPostIdAndUserId(POST_ID, USER_ID))
                    .willReturn(Optional.empty());
            given(communityPostNotificationSettingRepository.save(any()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            communityNotificationService.updateSetting(
                    CommunityPostNotificationSettingCommand.of(USER_ID, POST_ID, false, true));

            ArgumentCaptor<CommunityPostNotificationSetting> captor =
                    ArgumentCaptor.forClass(CommunityPostNotificationSetting.class);
            then(communityPostNotificationSettingRepository).should().save(captor.capture());
            assertSoftly(softly -> {
                softly.assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
                softly.assertThat(captor.getValue().getCommunityPostId()).isEqualTo(POST_ID);
                softly.assertThat(captor.getValue().isNotifyComments()).isFalse();
                softly.assertThat(captor.getValue().isNotifyReplies()).isTrue();
            });
        }

        @Test
        void 이미_행이_있으면_새로_만들지_않고_값만_바꾼다() {
            CommunityPostNotificationSetting setting =
                    CommunityPostNotificationSetting.createSetting(USER_ID, POST_ID, true, false);
            given(communityPostRepository.getByPostId(POST_ID)).willReturn(POST(USER_ID));
            given(communityPostNotificationSettingRepository.findByCommunityPostIdAndUserId(POST_ID, USER_ID))
                    .willReturn(Optional.of(setting));

            communityNotificationService.updateSetting(
                    CommunityPostNotificationSettingCommand.of(USER_ID, POST_ID, false, true));

            then(communityPostNotificationSettingRepository).should(never()).save(any());
            assertSoftly(softly -> {
                softly.assertThat(setting.isNotifyComments()).isFalse();
                softly.assertThat(setting.isNotifyReplies()).isTrue();
            });
        }

        @Test
        void 존재하지_않는_게시글이면_설정을_바꿀_수_없다() {
            given(communityPostRepository.getByPostId(POST_ID))
                    .willThrow(new ApplicationException(CommunityPostException.POST_NOT_FOUND));

            assertThatThrownBy(() -> communityNotificationService.updateSetting(
                    CommunityPostNotificationSettingCommand.of(USER_ID, POST_ID, false, true)))
                    .isInstanceOf(ApplicationException.class);
        }
    }

    @Nested
    @DisplayName("댓글 알림 생성 - 루트 댓글")
    class CreateNotificationsOnRootComment {

        @Test
        void 게시글_작성자에게_댓글_알림을_남긴다() {
            givenPost();
            givenSettings();

            List<Long> recipients = createRecipients(
                    comment(10L, null, COMMENTER_ID));

            List<CommunityNotification> saved = captureSaved();
            assertSoftly(softly -> {
                softly.assertThat(recipients).containsExactly(USER_ID);
                softly.assertThat(saved).hasSize(1);
                softly.assertThat(saved.getFirst().getUserId()).isEqualTo(USER_ID);
                softly.assertThat(saved.getFirst().getType()).isEqualTo(CommunityNotificationType.COMMENT);
            });
        }

        @Test
        void 발생_시점의_게시글_제목과_댓글_내용을_복사해_둔다() {
            CommunityPost post = givenPost();
            givenSettings();

            communityNotificationService.createNotifications(comment(10L, null, COMMENTER_ID));

            List<CommunityNotification> saved = captureSaved();
            assertSoftly(softly -> {
                softly.assertThat(saved.getFirst().getPostTitle()).isEqualTo(post.getTitle());
                softly.assertThat(saved.getFirst().getContent()).isEqualTo("몇 시에 만나나요?");
                softly.assertThat(saved.getFirst().getCommunityPostId()).isEqualTo(POST_ID);
            });
        }

        @Test
        void 내_글에_내가_댓글을_달면_알림이_없다() {
            givenPost();
            givenSettings();

            List<Long> recipients = createRecipients(
                    comment(10L, null, USER_ID));

            assertThat(recipients).isEmpty();
            then(communityNotificationRepository).should(never()).saveAll(any());
        }

        @Test
        void 게시글_작성자가_댓글_알림을_끄면_보내지_않는다() {
            givenPost();
            givenSettings(setting(USER_ID, false, false));

            List<Long> recipients = createRecipients(
                    comment(10L, null, COMMENTER_ID));

            assertThat(recipients).isEmpty();
            then(communityNotificationRepository).should(never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("댓글 알림 생성 - 답글")
    class CreateNotificationsOnReply {

        @Test
        void 부모_댓글_작성자에게_답글_알림을_남긴다() {
            givenPost();
            givenParentComment(10L, PARENT_WRITER_ID);
            givenSettings();

            communityNotificationService.createNotifications(comment(11L, 10L, COMMENTER_ID));

            List<CommunityNotification> saved = captureSaved();
            assertSoftly(softly -> {
                softly.assertThat(saved).hasSize(1);
                softly.assertThat(saved.getFirst().getUserId()).isEqualTo(PARENT_WRITER_ID);
                softly.assertThat(saved.getFirst().getType()).isEqualTo(CommunityNotificationType.REPLY);
            });
        }

        @Test
        void 게시글_작성자는_기본으로_답글_알림을_받지_않는다() {
            givenPost();
            givenParentComment(10L, PARENT_WRITER_ID);
            givenSettings();

            List<Long> recipients = createRecipients(
                    comment(11L, 10L, COMMENTER_ID));

            assertThat(recipients).doesNotContain(USER_ID);
        }

        @Test
        void 게시글_작성자가_답글_알림을_켜면_받는다() {
            givenPost();
            givenParentComment(10L, PARENT_WRITER_ID);
            givenSettings(setting(USER_ID, true, true));

            List<Long> recipients = createRecipients(
                    comment(11L, 10L, COMMENTER_ID));

            assertThat(recipients).containsExactlyInAnyOrder(PARENT_WRITER_ID, USER_ID);
        }

        @Test
        void 게시글_작성자가_부모_댓글도_썼으면_한_건만_남긴다() {
            givenPost();
            givenParentComment(10L, USER_ID);
            givenSettings();

            List<Long> recipients = createRecipients(
                    comment(11L, 10L, COMMENTER_ID));

            assertThat(recipients).containsExactly(USER_ID);
        }

        @Test
        void 부모_댓글의_알림을_끄면_답글_알림이_가지_않는다() {
            givenPost();
            givenParentComment(10L, PARENT_WRITER_ID, false);
            givenSettings();

            List<Long> recipients = createRecipients(
                    comment(11L, 10L, COMMENTER_ID));

            assertThat(recipients).isEmpty();
        }

        @Test
        void 글_알림을_꺼도_내_댓글에_달린_답글_알림은_온다() {
            givenPost();
            givenParentComment(10L, PARENT_WRITER_ID);
            givenSettings(setting(PARENT_WRITER_ID, false, false));

            List<Long> recipients = createRecipients(
                    comment(11L, 10L, COMMENTER_ID));

            assertThat(recipients).containsExactly(PARENT_WRITER_ID);
        }

        @Test
        void 같은_글의_다른_댓글_알림은_서로_영향을_주지_않는다() {
            givenPost();
            givenParentComment(10L, PARENT_WRITER_ID, false);
            givenSettings();

            List<Long> muted = createRecipients(comment(11L, 10L, COMMENTER_ID));
            givenParentComment(20L, PARENT_WRITER_ID, true);
            List<Long> alive = createRecipients(comment(21L, 20L, COMMENTER_ID));

            assertSoftly(softly -> {
                softly.assertThat(muted).isEmpty();
                softly.assertThat(alive).containsExactly(PARENT_WRITER_ID);
            });
        }

        @Test
        void 내_댓글에_내가_답글을_달면_알림이_없다() {
            givenPost();
            givenParentComment(10L, COMMENTER_ID);
            givenSettings();

            List<Long> recipients = createRecipients(
                    comment(11L, 10L, COMMENTER_ID));

            assertThat(recipients).isEmpty();
        }
    }

    @Nested
    @DisplayName("댓글 알림 생성 - 제3자")
    class CreateNotificationsForThirdParty {

        @Test
        void 그_글을_명시적으로_켠_사람도_받는다() {
            givenPost();
            givenSettings(setting(THIRD_PARTY_ID, true, false));

            List<Long> recipients = createRecipients(
                    comment(10L, null, COMMENTER_ID));

            assertThat(recipients).containsExactlyInAnyOrder(USER_ID, THIRD_PARTY_ID);
        }

        @Test
        void 켜지_않은_사람은_설정_행이_없어_받지_않는다() {
            givenPost();
            givenSettings();

            List<Long> recipients = createRecipients(
                    comment(10L, null, COMMENTER_ID));

            assertThat(recipients).containsExactly(USER_ID);
        }

        @Test
        void 답글은_답글_설정을_켠_사람만_받는다() {
            givenPost();
            givenParentComment(10L, PARENT_WRITER_ID);
            givenSettings(setting(THIRD_PARTY_ID, true, false));

            List<Long> recipients = createRecipients(
                    comment(11L, 10L, COMMENTER_ID));

            assertThat(recipients).doesNotContain(THIRD_PARTY_ID);
        }
    }
}
