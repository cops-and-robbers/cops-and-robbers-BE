package com.team.cops_and_robbers.community.post.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.InfrastructureException;
import com.team.cops_and_robbers.community.chat.common.repository.CommunityChatMessageRepository;
import com.team.cops_and_robbers.community.chat.member.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.chat.member.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.community.chat.pin.repository.CommunityChatPinRepository;
import com.team.cops_and_robbers.community.comment.repository.CommunityCommentRepository;
import com.team.cops_and_robbers.community.notification.application.dto.result.CommunityPostNotificationSettingResult;
import com.team.cops_and_robbers.community.notification.domain.CommunityPostNotificationRole;
import com.team.cops_and_robbers.community.notification.repository.CommunityNotificationRepository;
import com.team.cops_and_robbers.community.notification.repository.CommunityPostNotificationSettingRepository;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostRow;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostSearchCondition;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostCreateCommand;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostDeleteCommand;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostListCommand;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostStatusCommand;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostUpdateCommand;
import com.team.cops_and_robbers.community.post.application.dto.result.CommunityPostCursorResult;
import com.team.cops_and_robbers.community.post.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.post.domain.CommunityPost;
import com.team.cops_and_robbers.community.post.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.post.domain.PostAddress;
import com.team.cops_and_robbers.community.post.exception.CommunityPostException;
import com.team.cops_and_robbers.community.post.infrastructure.GeocodingClient;
import com.team.cops_and_robbers.community.post.infrastructure.GeocodingResult;
import com.team.cops_and_robbers.community.post.repository.CommunityPostCountProjection;
import com.team.cops_and_robbers.community.post.repository.CommunityPostRepository;
import com.team.cops_and_robbers.community.reaction.application.dto.CommunityPostReactionCounts;
import com.team.cops_and_robbers.community.reaction.repository.CommunityPostLikeRepository;
import com.team.cops_and_robbers.community.reaction.repository.CommunityPostScrapRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPostService {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityChatMemberRepository communityChatMemberRepository;
    private final CommunityChatPinRepository communityChatPinRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final CommunityPostScrapRepository communityPostScrapRepository;
    private final CommunityNotificationRepository communityNotificationRepository;
    private final CommunityPostNotificationSettingRepository communityPostNotificationSettingRepository;
    private final UserRepository userRepository;
    private final GeocodingClient geocodingClient;
    private final Clock clock;

    @Transactional
    public CommunityPostResult createPost(CommunityPostCreateCommand command) {
        validateMeetingDate(command.meetingAt());
        User writer = userRepository.getByUserId(command.writerId());

        PostAddress postAddress = resolveAddress(command.latitude(), command.longitude());
        CommunityPost post = communityPostRepository.save(CommunityPost.createPost(command, postAddress));
        communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), command.writerId()));
        return CommunityPostResult.from(post, writer, true, CommunityPostReactionCounts.EMPTY);
    }

    /**
     * requesterId는 비로그인 조회면 null로 들어오고, isLikedByRequester·isScrappedByRequester는 항상 false다.
     */
    public CommunityPostCursorResult getPostList(CommunityPostListCommand command, Long requesterId) {
        String countryScopeKey = command.countryScopeKey();

        CommunityPostSearchCondition condition = new CommunityPostSearchCondition(
                command.countryCode(), command.excludeCountryCodes(),
                command.sort(), command.latitude(), command.longitude(), command.keyword());
        List<CommunityPostRow> fetched = communityPostRepository.findPage(
                condition,
                CommunityPostCursor.decode(command.cursor(), countryScopeKey, command.sort(), command.keyword())
                        .orElse(null),
                command.size());

        boolean hasNext = fetched.size() > command.size();
        List<CommunityPostRow> rows = hasNext ? fetched.subList(0, command.size()) : fetched;
        List<CommunityPost> posts = rows.stream().map(CommunityPostRow::post).toList();

        return new CommunityPostCursorResult(
                toResults(posts, requesterId),
                resolveNextCursor(rows, hasNext, countryScopeKey, command),
                hasNext
        );
    }


    /**
     * requesterId는 비로그인 조회면 null로 들어오고, chatJoined·isLikedByRequester·isScrappedByRequester는 항상 false다.
     */
    public CommunityPostResult getPost(Long postId, Long requesterId) {
        CommunityPost post = communityPostRepository.getByPostId(postId);
        boolean chatJoined = requesterId != null
                && communityChatMemberRepository.existsByCommunityPostIdAndUserId(postId, requesterId);
        return CommunityPostResult.of(
                post, findWriter(post.getWriterId()), chatJoined, findNotificationSettings(post, requesterId),
                reactionCountsFor(postId, requesterId));
    }

    @Transactional
    public CommunityPostResult updatePost(CommunityPostUpdateCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateAuthor(post, command.writerId());
        validateMeetingDate(command.meetingAt());
        User writer = userRepository.getByUserId(command.writerId());

        PostAddress postAddress = resolveUpdatedAddress(post, command);
        post.updatePost(command, postAddress);
        return CommunityPostResult.from(
                post, writer, true, reactionCountsFor(post.getId(), command.writerId()));
    }

    /**
     * 채팅 데이터를 지우는 동안 참여 요청이 끼어들면 게시글 없는 멤버 행이 남는다.
     * 참여와 같은 게시글 행 락을 잡아 두 흐름이 겹치지 않게 한다.
     */
    @Transactional
    public void deletePost(CommunityPostDeleteCommand command) {
        CommunityPost post = communityPostRepository.getByPostIdForUpdate(command.postId());
        validateAuthor(post, command.writerId());
        communityChatMessageRepository.deleteAllByCommunityPostId(command.postId());
        communityChatMemberRepository.deleteAllByCommunityPostId(command.postId());
        communityChatPinRepository.deleteByCommunityPostId(command.postId());
        communityCommentRepository.deleteAllByCommunityPostId(command.postId());
        communityPostLikeRepository.deleteAllByCommunityPostId(command.postId());
        communityPostScrapRepository.deleteAllByCommunityPostId(command.postId());
        communityNotificationRepository.deleteAllByCommunityPostId(command.postId());
        communityPostNotificationSettingRepository.deleteAllByCommunityPostId(command.postId());
        communityPostRepository.deleteByPostId(command.postId());
    }

    @Transactional
    public CommunityPostResult updateStatus(CommunityPostStatusCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateAuthor(post, command.writerId());
        post.updateStatus(command.status());
        return CommunityPostResult.from(
                post, findWriter(post.getWriterId()), true, reactionCountsFor(post.getId(), command.writerId()));
    }

    /** 비로그인 조회는 보여줄 설정이 없어 null로 내려간다. */
    private CommunityPostNotificationSettingResult findNotificationSettings(CommunityPost post, Long requesterId) {
        if (requesterId == null) {
            return null;
        }
        return communityPostNotificationSettingRepository
                .findByCommunityPostIdAndUserId(post.getId(), requesterId)
                .map(CommunityPostNotificationSettingResult::from)
                .orElseGet(() -> CommunityPostNotificationSettingResult.from(roleOf(post, requesterId)));
    }

    private CommunityPostNotificationRole roleOf(CommunityPost post, Long userId) {
        return post.getWriterId().equals(userId)
                ? CommunityPostNotificationRole.POST_WRITER
                : CommunityPostNotificationRole.OTHER;
    }

    private void validateAuthor(CommunityPost post, Long writerId) {
        if (!post.getWriterId().equals(writerId)) {
            throw new ApplicationException(CommunityPostException.FORBIDDEN_NOT_AUTHOR);
        }
    }

    private void validateMeetingDate(LocalDateTime meetingAt) {
        if (meetingAt.isBefore(LocalDateTime.now(clock))) {
            throw new ApplicationException(CommunityPostException.INVALID_MEETING_DATE);
        }
    }

    private PostAddress resolveUpdatedAddress(CommunityPost post, CommunityPostUpdateCommand command) {
        boolean coordinatesChanged = !post.getLatitude().equals(command.latitude())
                || !post.getLongitude().equals(command.longitude());
        PostAddress savedAddress = post.getPostAddress();
        if (!coordinatesChanged && !savedAddress.isEmpty()) {
            return savedAddress;
        }
        return resolveAddress(command.latitude(), command.longitude());
    }

    /**
     * 실패하면 게시글을 만들지 않는다. 주소 없이 저장하면 countryCode가 비어 어느 국가 목록에도 걸리지 않아,
     * 작성자 본인도 찾을 수 없는 글이 된다. 조용히 사라지는 것보다 작성 시점에 실패를 알리는 편이 낫다.
     */
    private PostAddress resolveAddress(Double latitude, Double longitude) {
        return switch (geocodingClient.reverseGeocode(latitude, longitude)) {
            case GeocodingResult.Resolved resolved -> resolved.postAddress();
            case GeocodingResult.NotFound ignored ->
                    throw new ApplicationException(CommunityPostException.ADDRESS_NOT_FOUND);
            case GeocodingResult.Failed ignored ->
                    throw new InfrastructureException(CommunityPostException.ADDRESS_LOOKUP_FAILED);
        };
    }

    private User findWriter(Long writerId) {
        return userRepository.findById(writerId).orElse(null);
    }

    /**
     * 목록은 채팅방 참여 여부를 알려줄 컨텍스트가 없어 chatJoined를 항상 false로 내려준다.
     * likeCount·scrapCount·isLikedByRequester·isScrappedByRequester는 요청자 기준으로 실조회한다.
     */
    private List<CommunityPostResult> toResults(List<CommunityPost> posts, Long requesterId) {
        Map<Long, User> writers = findWriters(posts);
        List<Long> postIds = posts.stream().map(CommunityPost::getId).toList();
        Map<Long, CommunityPostReactionCounts> reactions = reactionCountsFor(postIds, requesterId);

        return posts.stream()
                .map(post -> CommunityPostResult.from(
                        post, writers.get(post.getWriterId()), false,
                        reactions.getOrDefault(post.getId(), CommunityPostReactionCounts.EMPTY)))
                .toList();
    }

    /** 단건 조회·수정·상태변경에서 쓰는 단일 게시글 좋아요·스크랩 조회 */
    private CommunityPostReactionCounts reactionCountsFor(Long postId, Long requesterId) {
        long likeCount = communityPostLikeRepository.countByCommunityPostId(postId);
        long scrapCount = communityPostScrapRepository.countByCommunityPostId(postId);
        boolean isLikedByRequester = (requesterId != null)
                && communityPostLikeRepository.existsByCommunityPostIdAndUserId(postId, requesterId);
        boolean isScrappedByRequester = (requesterId != null)
                && communityPostScrapRepository.existsByCommunityPostIdAndUserId(postId, requesterId);

        return new CommunityPostReactionCounts(likeCount, scrapCount, isLikedByRequester, isScrappedByRequester);
    }

    /**
     * 목록 조회에서 쓰는 배치 좋아요·스크랩 조회
     */
    private Map<Long, CommunityPostReactionCounts> reactionCountsFor(List<Long> postIds, Long requesterId) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> likeCounts = communityPostLikeRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(CommunityPostCountProjection::postId, CommunityPostCountProjection::count));
        Map<Long, Long> scrapCounts = communityPostScrapRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(CommunityPostCountProjection::postId, CommunityPostCountProjection::count));

        Set<Long> likedPostIds;
        Set<Long> scrappedPostIds;

        if (requesterId == null) {
            likedPostIds = Set.of();
            scrappedPostIds = Set.of();
        }
        else {
            likedPostIds = Set.copyOf(communityPostLikeRepository.findLikedPostIds(requesterId, postIds));
            scrappedPostIds = Set.copyOf(communityPostScrapRepository.findScrappedPostIds(requesterId, postIds));
        }

        return postIds.stream().distinct().collect(Collectors.toMap(
                Function.identity(),
                postId -> new CommunityPostReactionCounts(
                        likeCounts.getOrDefault(postId, 0L),
                        scrapCounts.getOrDefault(postId, 0L),
                        likedPostIds.contains(postId),
                        scrappedPostIds.contains(postId))));
    }

    private String resolveNextCursor(
            List<CommunityPostRow> rows, boolean hasNext, String countryScopeKey, CommunityPostListCommand command) {
        if (!hasNext) {
            return null;
        }
        CommunityPostRow last = rows.getLast();
        CommunityPost post = last.post();
        return CommunityPostCursor.encode(countryScopeKey, command.sort(), command.keyword(),
                post.isClosed(), sortKeyOf(last, command.sort()), post.getId());
    }

    private String sortKeyOf(CommunityPostRow row, CommunityPostSort sort) {
        return switch (sort) {
            case DEADLINE -> CommunityPostCursor.sortKeyOf(row.post().getMeetingAt());
            case DISTANCE -> String.valueOf(row.distance());
            case POPULAR -> String.valueOf(row.score());
            default -> CommunityPostCursor.sortKeyOf(row.post().getCreatedAt());
        };
    }

    private Map<Long, User> findWriters(List<CommunityPost> posts) {
        List<Long> writerIds = posts.stream()
                .map(CommunityPost::getWriterId)
                .distinct()
                .toList();
        return userRepository.findAllById(writerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
