package com.team.cops_and_robbers.community.comment.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.comment.domain.CommunityComment;
import com.team.cops_and_robbers.user.domain.User;

import java.util.List;

public record CommunityCommentResult(
        Long id,
        Long parentId,
        Long writerId,
        String writerNickname,
        Integer writerProfileIcon,
        String content,
        boolean deleted,
        boolean replyNotificationsEnabled,
        String createdAt,
        String updatedAt,
        List<CommunityCommentResult> replies
) {
    public static CommunityCommentResult of(
            CommunityComment comment,
            User writer,
            List<CommunityCommentResult> replies
    ) {
        boolean deleted = comment.isDeleted();
        return new CommunityCommentResult(
                comment.getId(),
                comment.getParentId(),
                deleted ? null : comment.getWriterId(),
                deleted ? null : (writer != null ? writer.getNickname() : User.UNKNOWN_NICKNAME),
                deleted ? null : (writer != null ? writer.getProfileIcon() : User.DEFAULT_PROFILE_ICON),
                deleted ? null : comment.getContent(),
                deleted,
                comment.isReplyNotificationsEnabled(),
                TimestampUtil.toIsoString(comment.getCreatedAt()),
                TimestampUtil.toIsoString(comment.getUpdatedAt()),
                replies
        );
    }

    public static CommunityCommentResult from(CommunityComment comment, User writer) {
        return of(comment, writer, List.of());
    }
}
