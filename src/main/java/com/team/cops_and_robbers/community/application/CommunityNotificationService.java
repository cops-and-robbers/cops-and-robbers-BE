package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.community.application.dto.CommunityNotificationDispatch;
import com.team.cops_and_robbers.community.application.dto.CommunityNotificationPush;
import com.team.cops_and_robbers.community.application.dto.command.CommunityNotificationListCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostNotificationSettingCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityNotificationListResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityNotificationResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityNotificationUnreadCountResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostNotificationSettingResult;
import com.team.cops_and_robbers.community.domain.CommunityComment;
import com.team.cops_and_robbers.community.domain.CommunityNotification;
import com.team.cops_and_robbers.community.domain.CommunityNotificationType;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.CommunityPostNotificationRole;
import com.team.cops_and_robbers.community.domain.CommunityPostNotificationSetting;
import com.team.cops_and_robbers.community.repository.CommunityCommentRepository;
import com.team.cops_and_robbers.community.repository.CommunityNotificationRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostNotificationSettingRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityNotificationService {

    /** 조회에서 잘라내기만 하고 행은 지우지 않는다. */
    private static final int RETENTION_DAYS = 60;

    private final CommunityNotificationRepository communityNotificationRepository;
    private final CommunityPostNotificationSettingRepository communityPostNotificationSettingRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public CommunityNotificationListResult getNotifications(CommunityNotificationListCommand command) {
        User user = userRepository.getByUserId(command.userId());

        List<CommunityNotification> fetched = communityNotificationRepository.findPageByCursor(
                command.userId(), retentionThreshold(), command.cursor(), command.toPageable());

        boolean hasNext = fetched.size() > command.size();
        List<CommunityNotification> notifications = hasNext ? fetched.subList(0, command.size()) : fetched;

        List<CommunityNotificationResult> content = notifications.stream()
                .map(notification -> CommunityNotificationResult.of(
                        notification, user.getCommunityNotificationReadAt()))
                .toList();
        Long nextCursor = hasNext ? notifications.getLast().getId() : null;

        return new CommunityNotificationListResult(content, nextCursor, hasNext);
    }

    public CommunityNotificationUnreadCountResult getUnreadCount(Long userId) {
        User user = userRepository.getByUserId(userId);
        return CommunityNotificationUnreadCountResult.from(communityNotificationRepository.countUnread(
                userId, retentionThreshold(), user.getCommunityNotificationReadAt()));
    }

    @Transactional
    public void readNotifications(Long userId) {
        userRepository.getByUserId(userId).readCommunityNotifications(LocalDateTime.now(clock));
    }

    @Transactional
    public CommunityPostNotificationSettingResult updateSetting(CommunityPostNotificationSettingCommand command) {
        communityPostRepository.getByPostId(command.postId());

        CommunityPostNotificationSetting setting = communityPostNotificationSettingRepository
                .findByCommunityPostIdAndUserId(command.postId(), command.userId())
                .orElseGet(() -> communityPostNotificationSettingRepository.save(
                        CommunityPostNotificationSetting.createSetting(
                                command.userId(), command.postId(),
                                command.notifyComments(), command.notifyReplies())));

        setting.updateSetting(command.notifyComments(), command.notifyReplies());
        return CommunityPostNotificationSettingResult.from(setting);
    }

    /**
     * 받을 사람을 발생 시점에 확정해 저장하고 그 목록을 돌려준다.
     * 조회 시점에 다시 계산하면 알림을 켜기 전의 댓글까지 소급되고, 푸시와 알림함이 서로 다른 계산을 하게 된다.
     * 댓글 트랜잭션이 이미 커밋된 뒤에 불리므로 별도 트랜잭션으로 열고, 이 메서드가 반환하면 저장은 커밋된 상태다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommunityNotificationDispatch createNotifications(CommunityComment comment) {
        CommunityPost post = communityPostRepository.getByPostId(comment.getCommunityPostId());

        List<Long> recipients = resolveRecipients(comment, post);
        if (recipients.isEmpty()) {
            return CommunityNotificationDispatch.none();
        }
        communityNotificationRepository.saveAll(toNotifications(recipients, comment, post));
        return new CommunityNotificationDispatch(
                recipients, CommunityNotificationPush.of(typeOf(comment), post, comment));
    }

    private List<Long> resolveRecipients(CommunityComment comment, CommunityPost post) {
        Map<Long, CommunityPostNotificationSetting> settings = findSettings(post.getId());
        CommunityNotificationType type = typeOf(comment);

        Stream<Long> byPostSetting = candidates(post, settings.keySet()).stream()
                .filter(candidate -> allows(settings.get(candidate.userId()), candidate.role(), type))
                .map(Candidate::userId);

        return Stream.concat(parentCommentWriter(comment).stream(), byPostSetting)
                .filter(userId -> !comment.isWrittenBy(userId))
                .distinct()
                .toList();
    }

    /** 부모 댓글 작성자는 그 댓글의 설정만 본다. 게시글 알림을 꺼도 여기는 영향받지 않는다. */
    private Optional<Long> parentCommentWriter(CommunityComment comment) {
        if (!comment.isReply()) {
            return Optional.empty();
        }
        CommunityComment parent = communityCommentRepository.getByCommentId(comment.getParentId());
        return parent.isNotifyReplies() ? Optional.of(parent.getWriterId()) : Optional.empty();
    }

    /** 제3자는 그 글을 명시적으로 켠 사람만 대상이라 설정 행이 있는 경우뿐이다. */
    private List<Candidate> candidates(CommunityPost post, Set<Long> settingUserIds) {
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(new Candidate(post.getWriterId(), CommunityPostNotificationRole.POST_WRITER));

        settingUserIds.stream()
                .filter(userId -> !userId.equals(post.getWriterId()))
                .forEach(userId -> candidates.add(new Candidate(userId, CommunityPostNotificationRole.OTHER)));

        return candidates;
    }

    private boolean allows(
            CommunityPostNotificationSetting setting,
            CommunityPostNotificationRole role,
            CommunityNotificationType type
    ) {
        return setting != null ? setting.allows(type) : role.allows(type);
    }

    private Map<Long, CommunityPostNotificationSetting> findSettings(Long postId) {
        return communityPostNotificationSettingRepository.findAllByCommunityPostId(postId).stream()
                .collect(Collectors.toMap(CommunityPostNotificationSetting::getUserId, Function.identity()));
    }

    private List<CommunityNotification> toNotifications(
            List<Long> recipients,
            CommunityComment comment,
            CommunityPost post
    ) {
        return recipients.stream()
                .map(userId -> CommunityNotification.createNotification(
                        userId, typeOf(comment), post.getId(), post.getTitle(), comment.getContent()))
                .toList();
    }

    private CommunityNotificationType typeOf(CommunityComment comment) {
        return comment.isReply() ? CommunityNotificationType.REPLY : CommunityNotificationType.COMMENT;
    }

    private LocalDateTime retentionThreshold() {
        return LocalDateTime.now(clock).minusDays(RETENTION_DAYS);
    }

    private record Candidate(Long userId, CommunityPostNotificationRole role) {}
}
