package com.team.cops_and_robbers.community.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.domain.CommunityComment;

import java.util.List;

public record CommunityCommentResult(
        Long id,
        Long parentId,
        Long writerId,
        String writerNickname,
        String content,
        boolean deleted,
        String createdAt,
        String updatedAt,
        List<CommunityCommentResult> replies
) {
    public static CommunityCommentResult of(
            CommunityComment comment,
            String writerNickname,
            List<CommunityCommentResult> replies
    ) {
        boolean deleted = comment.isDeleted();
        return new CommunityCommentResult(
                comment.getId(),
                comment.getParentId(),
                deleted ? null : comment.getWriterId(),
                deleted ? null : writerNickname,
                deleted ? null : comment.getContent(),
                deleted,
                TimestampUtil.toIsoString(comment.getCreatedAt()),
                TimestampUtil.toIsoString(comment.getUpdatedAt()),
                replies
        );
    }

    public static CommunityCommentResult from(CommunityComment comment, String writerNickname) {
        return of(comment, writerNickname, List.of());
    }
}
