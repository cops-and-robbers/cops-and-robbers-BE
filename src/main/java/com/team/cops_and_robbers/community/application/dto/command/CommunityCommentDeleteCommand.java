package com.team.cops_and_robbers.community.application.dto.command;

public record CommunityCommentDeleteCommand(
        Long writerId,
        Long commentId
) {
    public static CommunityCommentDeleteCommand of(Long writerId, Long commentId) {
        return new CommunityCommentDeleteCommand(writerId, commentId);
    }
}
