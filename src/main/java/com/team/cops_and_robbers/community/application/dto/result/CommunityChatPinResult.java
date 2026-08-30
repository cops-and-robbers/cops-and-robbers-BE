package com.team.cops_and_robbers.community.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.domain.CommunityChatPin;

public record CommunityChatPinResult(
        Long id,
        Long postId,
        Long writerId,
        String writerNickname,
        int writerProfileIcon,
        String content,
        String createdAt,
        String updatedAt
) {
    public static CommunityChatPinResult of(CommunityChatPin pin, String writerNickname, int writerProfileIcon) {
        return new CommunityChatPinResult(
                pin.getId(),
                pin.getCommunityPostId(),
                pin.getWriterId(),
                writerNickname,
                writerProfileIcon,
                pin.getContent(),
                TimestampUtil.toIsoString(pin.getCreatedAt()),
                TimestampUtil.toIsoString(pin.getUpdatedAt())
        );
    }

    public static CommunityChatPinResult empty(Long postId) {
        return new CommunityChatPinResult(null, postId, null, null, 0, null, null, null);
    }
}
