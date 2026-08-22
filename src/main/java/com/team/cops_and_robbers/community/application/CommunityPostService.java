package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.InfrastructureException;
import com.team.cops_and_robbers.community.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.application.dto.CommunityPostSearchCondition;
import com.team.cops_and_robbers.community.application.dto.CommunityPostRow;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostCreateCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostDeleteCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostListCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostStatusCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostUpdateCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostCursorResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.domain.PostAddress;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.infrastructure.GeocodingClient;
import com.team.cops_and_robbers.community.infrastructure.GeocodingResult;
import com.team.cops_and_robbers.community.repository.CommunityChatMemberRepository;
import com.team.cops_and_robbers.community.repository.CommunityChatMessageRepository;
import com.team.cops_and_robbers.community.repository.CommunityPostRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPostService {

    private final CommunityPostRepository communityPostRepository;
    private final CommunityChatMemberRepository communityChatMemberRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;
    private final UserRepository userRepository;
    private final GeocodingClient geocodingClient;

    @Transactional
    public CommunityPostResult createPost(CommunityPostCreateCommand command) {
        validateMeetingDate(command.meetingAt());
        PostAddress postAddress = resolveAddress(command.latitude(), command.longitude());

        User writer = userRepository.getByUserId(command.writerId());
        CommunityPost post = communityPostRepository.save(CommunityPost.createPost(command, postAddress));
        communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), command.writerId()));
        return CommunityPostResult.from(post, writer.getNickname());
    }

    public CommunityPostCursorResult getPostList(CommunityPostListCommand command) {
        String countryCode = command.countryCode().toUpperCase(Locale.ROOT);

        CommunityPostSearchCondition condition = new CommunityPostSearchCondition(
                countryCode, command.sort(), command.latitude(), command.longitude(), command.keyword());
        List<CommunityPostRow> fetched = communityPostRepository.findPage(
                condition,
                CommunityPostCursor.decode(command.cursor(), countryCode, command.sort()).orElse(null),
                command.size());

        boolean hasNext = fetched.size() > command.size();
        List<CommunityPostRow> rows = hasNext ? fetched.subList(0, command.size()) : fetched;
        List<CommunityPost> posts = rows.stream().map(CommunityPostRow::post).toList();

        return new CommunityPostCursorResult(
                toResults(posts), resolveNextCursor(rows, hasNext, countryCode, command.sort()), hasNext);
    }


    public CommunityPostResult getPost(Long postId) {
        CommunityPost post = communityPostRepository.getByPostId(postId);
        return CommunityPostResult.from(post, findWriterNickname(post.getWriterId()));
    }

    @Transactional
    public CommunityPostResult updatePost(CommunityPostUpdateCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateAuthor(post, command.writerId());
        validateMeetingDate(command.meetingAt());
        PostAddress postAddress = resolveUpdatedAddress(post, command);
        post.updatePost(command, postAddress);
        return CommunityPostResult.from(post, findWriterNickname(post.getWriterId()));
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
        communityPostRepository.deleteByPostId(command.postId());
    }

    @Transactional
    public CommunityPostResult updateStatus(CommunityPostStatusCommand command) {
        CommunityPost post = communityPostRepository.getByPostId(command.postId());
        validateAuthor(post, command.writerId());
        post.updateStatus(command.status());
        return CommunityPostResult.from(post, findWriterNickname(post.getWriterId()));
    }

    private void validateAuthor(CommunityPost post, Long writerId) {
        if (!post.getWriterId().equals(writerId)) {
            throw new ApplicationException(CommunityPostException.FORBIDDEN_NOT_AUTHOR);
        }
    }

    private void validateMeetingDate(LocalDateTime meetingAt) {
        if (meetingAt.isBefore(LocalDateTime.now())) {
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

    private String findWriterNickname(Long writerId) {
        return userRepository.findById(writerId)
                .map(User::getNickname)
                .orElse(null);
    }

    private List<CommunityPostResult> toResults(List<CommunityPost> posts) {
        Map<Long, String> nicknames = findWriterNicknames(posts);
        return posts.stream()
                .map(post -> CommunityPostResult.from(post, nicknames.get(post.getWriterId())))
                .toList();
    }

    private String resolveNextCursor(
            List<CommunityPostRow> rows, boolean hasNext, String countryCode, CommunityPostSort sort) {
        if (!hasNext) {
            return null;
        }
        CommunityPostRow last = rows.getLast();
        CommunityPost post = last.post();
        return CommunityPostCursor.encode(countryCode, sort, post.isClosed(), sortKeyOf(last, sort), post.getId());
    }

    private String sortKeyOf(CommunityPostRow row, CommunityPostSort sort) {
        return switch (sort) {
            case DEADLINE -> CommunityPostCursor.sortKeyOf(row.post().getMeetingAt());
            case DISTANCE -> String.valueOf(row.distance());
            default -> CommunityPostCursor.sortKeyOf(row.post().getCreatedAt());
        };
    }

    private Map<Long, String> findWriterNicknames(List<CommunityPost> posts) {
        List<Long> writerIds = posts.stream()
                .map(CommunityPost::getWriterId)
                .distinct()
                .toList();
        return userRepository.findAllById(writerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
    }
}

