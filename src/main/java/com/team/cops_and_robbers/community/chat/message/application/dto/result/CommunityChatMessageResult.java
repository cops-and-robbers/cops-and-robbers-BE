package com.team.cops_and_robbers.community.chat.message.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.user.repository.UserProfileProjection;

public record CommunityChatMessageResult(
        Long id,
        String messageKey,
        Long senderId,
        String senderNickname,
        int senderProfileIcon,
        String message,
        CommunityChatMessageType messageType,
        String createdAt
) {
    /**
     * senderNickname·senderProfileIcon은 users의 현재 값을 우선하고, 탈퇴해 조회되지 않는 경우에만
     * 메시지에 박제된 발신 시점 값으로 대체한다.
     */
    public static CommunityChatMessageResult of(CommunityChatMessage message, UserProfileProjection currentProfile) {
        return new CommunityChatMessageResult(
                message.getId(),
                message.getMessageKey(),
                message.getSenderId(),
                currentProfile != null ? currentProfile.nickname() : message.getSenderNickname(),
                currentProfile != null ? currentProfile.profileIcon() : message.getSenderProfileIcon(),
                message.getMessage(),
                message.getMessageType(),
                TimestampUtil.toIsoString(message.getCreatedAt())
        );
    }
}
