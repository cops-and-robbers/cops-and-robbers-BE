package com.team.cops_and_robbers.community.reaction.application.dto;

/**
 * 좋아요·스크랩 카운트와 요청자 기준 여부
 */
public record CommunityPostReactionCounts(
        long likeCount,
        long scrapCount,
        boolean isLikedByRequester,
        boolean isScrappedByRequester
) {
    public static final CommunityPostReactionCounts EMPTY = new CommunityPostReactionCounts(0L, 0L, false, false);
}
