package com.team.cops_and_robbers.community.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;

public record CommunityChatMessageResult(
        Long id,
        String messageKey,
        Long senderId,
        String senderNickname,
        String message,
        CommunityChatMessageType messageType,
        String createdAt
) {
    /**
     * senderNickname은 users의 현재 닉네임을 우선하고, 탈퇴해 조회되지 않는 경우에만
     * 메시지에 박제된 발신 시점 닉네임으로 대체한다.
     */
    public static CommunityChatMessageResult of(CommunityChatMessage message, String currentNickname) {
        return new CommunityChatMessageResult(
                message.getId(),
                message.getMessageKey(),
                message.getSenderId(),
                currentNickname != null ? currentNickname : message.getSenderNickname(),
                message.getMessage(),
                message.getMessageType(),
                TimestampUtil.toIsoString(message.getCreatedAt())
        );
    }
}
