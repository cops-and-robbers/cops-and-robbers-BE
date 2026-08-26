package com.team.cops_and_robbers.community.application.dto.command;

public record CommunityCommentCreateCommand(
        Long writerId,
        Long postId,
        Long parentId,
        String content
) {
    public static CommunityCommentCreateCommand of(Long writerId, Long postId, Long parentId, String content) {
        return new CommunityCommentCreateCommand(writerId, postId, parentId, content);
    }
}
