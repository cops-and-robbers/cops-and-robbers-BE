package com.team.cops_and_robbers.community.domain;

import com.team.cops_and_robbers.common.util.TimestampUtil;

public record CommunityChatPinPayload(
        Long postId,
        Long pinId,
        Long writerId,
        String writerNickname,
        String content,
        CommunityChatSystemEventType action,
        String updatedAt
) {
    public static CommunityChatPinPayload of(CommunityChatPin pin, CommunityChatSystemEventType action, String writerNickname) {
        boolean deleted = action == CommunityChatSystemEventType.PIN_DELETED;

        return new CommunityChatPinPayload(
                pin.getCommunityPostId(),
                pin.getId(),
                pin.getWriterId(),
                writerNickname,
                deleted ? null : pin.getContent(),
                action,
                TimestampUtil.toIsoString(pin.getUpdatedAt())
        );
    }
}
