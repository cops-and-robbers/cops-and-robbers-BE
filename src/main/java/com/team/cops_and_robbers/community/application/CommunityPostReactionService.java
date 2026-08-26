package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostScrapListCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostScrapListResult;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.CommunityPostLike;
import com.team.cops_and_robbers.community.domain.CommunityPostScrap;
import com.team.cops_and_robbers.community.exception.CommunityPostReactionException;
import com.team.cops_and_robbers.community.repository.CommunityPostLikeRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostScrapRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPostReactionService {

    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final CommunityPostScrapRepository communityPostScrapRepository;
    private final CommunityPostRepository communityPostRepository;
    private final UserRepository userRepository;

    /**
     * exists 체크와 save 사이에도 동시에 두 번 눌린 요청이 둘 다 통과할 수 있다.
     * 최후 방어선은 유니크 제약이라, 거기서 막히면 DataIntegrityViolationException을 도메인 예외로 바꿔 던진다.
     */
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

    /** likePost와 같은 이유로 save를 try-catch로 감싼다. */
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
        Map<Long, String> nicknames = findWriterNicknames(postsById.values());

        List<CommunityPostResult> content = scraps.stream()
                .map(scrap -> postsById.get(scrap.getCommunityPostId()))
                .filter(Objects::nonNull)
                .map(post -> CommunityPostResult.from(post, nicknames.get(post.getWriterId())))
                .toList();
        Long nextCursor = hasNext ? scraps.getLast().getId() : null;

        return new CommunityPostScrapListResult(content, nextCursor, hasNext);
    }

    private Map<Long, CommunityPost> findPostsById(List<CommunityPostScrap> scraps) {
        List<Long> postIds = scraps.stream().map(CommunityPostScrap::getCommunityPostId).toList();
        return communityPostRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(CommunityPost::getId, post -> post));
    }

    private Map<Long, String> findWriterNicknames(Collection<CommunityPost> posts) {
        List<Long> writerIds = posts.stream().map(CommunityPost::getWriterId).distinct().toList();
        return userRepository.findAllById(writerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
    }
}
