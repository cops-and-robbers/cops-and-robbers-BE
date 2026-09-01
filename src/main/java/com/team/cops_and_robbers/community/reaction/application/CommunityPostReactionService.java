package com.team.cops_and_robbers.community.reaction.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.chat.member.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.community.post.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.post.domain.CommunityPost;
import com.team.cops_and_robbers.community.post.repository.CommunityPostCountProjection;
import com.team.cops_and_robbers.community.post.repository.CommunityPostRepository;
import com.team.cops_and_robbers.community.reaction.application.dto.CommunityPostReactionCounts;
import com.team.cops_and_robbers.community.reaction.application.dto.command.CommunityPostScrapListCommand;
import com.team.cops_and_robbers.community.reaction.application.dto.result.CommunityPostScrapListResult;
import com.team.cops_and_robbers.community.reaction.domain.CommunityPostLike;
import com.team.cops_and_robbers.community.reaction.domain.CommunityPostScrap;
import com.team.cops_and_robbers.community.reaction.exception.CommunityPostReactionException;
import com.team.cops_and_robbers.community.reaction.repository.CommunityPostLikeRepository;
import com.team.cops_and_robbers.community.reaction.repository.CommunityPostScrapRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPostReactionService {

    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final CommunityPostScrapRepository communityPostScrapRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityChatMemberRepository communityChatMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public void likePost(Long postId, Long userId) {
        communityPostRepository.getByPostId(postId);
        if (communityPostLikeRepository.existsByCommunityPostIdAndUserId(postId, userId)) {
            throw new ApplicationException(CommunityPostReactionException.ALREADY_LIKED);
        }
        try {
            communityPostLikeRepository.save(CommunityPostLike.createLike(postId, userId));
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(CommunityPostReactionException.ALREADY_LIKED);
        }
    }

    @Transactional
    public void unlikePost(Long postId, Long userId) {
        int deleted = communityPostLikeRepository.deleteByCommunityPostIdAndUserId(postId, userId);
        if (deleted == 0) {
            throw new ApplicationException(CommunityPostReactionException.LIKE_NOT_FOUND);
        }
    }

    @Transactional
    public void scrapPost(Long postId, Long userId) {
        communityPostRepository.getByPostId(postId);
        if (communityPostScrapRepository.existsByCommunityPostIdAndUserId(postId, userId)) {
            throw new ApplicationException(CommunityPostReactionException.ALREADY_SCRAPPED);
        }
        try {
            communityPostScrapRepository.save(CommunityPostScrap.createScrap(postId, userId));
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(CommunityPostReactionException.ALREADY_SCRAPPED);
        }
    }

    @Transactional
    public void unscrapPost(Long postId, Long userId) {
        int deleted = communityPostScrapRepository.deleteByCommunityPostIdAndUserId(postId, userId);
        if (deleted == 0) {
            throw new ApplicationException(CommunityPostReactionException.SCRAP_NOT_FOUND);
        }
    }

    public CommunityPostScrapListResult getMyScraps(CommunityPostScrapListCommand command) {
        List<CommunityPostScrap> fetched = communityPostScrapRepository.findPageByCursor(
                command.userId(), command.cursor(), command.toPageable());

        boolean hasNext = fetched.size() > command.size();
        List<CommunityPostScrap> scraps = hasNext ? fetched.subList(0, command.size()) : fetched;

        Map<Long, CommunityPost> postsById = findPostsById(scraps);
        Map<Long, User> writers = findWriters(postsById.values());
        Set<Long> joinedPostIds = Set.copyOf(communityChatMemberRepository.findPostIdsByUserId(command.userId()));
        Map<Long, CommunityPostReactionCounts> reactions = reactionCountsFor(postsById.keySet(), command.userId());

        List<CommunityPostResult> content = scraps.stream()
                .map(scrap -> postsById.get(scrap.getCommunityPostId()))
                .filter(Objects::nonNull)
                .map(post -> CommunityPostResult.from(
                        post, writers.get(post.getWriterId()), joinedPostIds.contains(post.getId()),
                        reactions.getOrDefault(post.getId(), CommunityPostReactionCounts.EMPTY)))
                .toList();
        Long nextCursor = hasNext ? scraps.getLast().getId() : null;

        return new CommunityPostScrapListResult(content, nextCursor, hasNext);
    }

    private Map<Long, CommunityPost> findPostsById(List<CommunityPostScrap> scraps) {
        List<Long> postIds = scraps.stream().map(CommunityPostScrap::getCommunityPostId).toList();
        return communityPostRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(CommunityPost::getId, post -> post));
    }

    private Map<Long, User> findWriters(Collection<CommunityPost> posts) {
        List<Long> writerIds = posts.stream().map(CommunityPost::getWriterId).distinct().toList();
        return userRepository.findAllById(writerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /**
     * 배치 좋아요·스크랩 조회
     * 내 스크랩 목록이라 isScrappedByRequester는 항상 true지만, isLikedByRequester는 실조회로 채운다.
     */
    private Map<Long, CommunityPostReactionCounts> reactionCountsFor(Collection<Long> postIds, Long requesterId) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> likeCounts = communityPostLikeRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(CommunityPostCountProjection::postId, CommunityPostCountProjection::count));
        Map<Long, Long> scrapCounts = communityPostScrapRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(CommunityPostCountProjection::postId, CommunityPostCountProjection::count));
        Set<Long> likedPostIds = Set.copyOf(communityPostLikeRepository.findLikedPostIds(requesterId, postIds));
        Set<Long> scrappedPostIds = Set.copyOf(communityPostScrapRepository.findScrappedPostIds(requesterId, postIds));

        return postIds.stream().distinct().collect(Collectors.toMap(
                Function.identity(),
                postId -> new CommunityPostReactionCounts(
                        likeCounts.getOrDefault(postId, 0L),
                        scrapCounts.getOrDefault(postId, 0L),
                        likedPostIds.contains(postId),
                        scrappedPostIds.contains(postId))));
    }
}
