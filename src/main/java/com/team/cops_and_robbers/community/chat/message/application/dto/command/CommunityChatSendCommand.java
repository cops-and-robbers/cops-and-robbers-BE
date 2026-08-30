package com.team.cops_and_robbers.community.chat.message.application.dto.command;

import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatGameInviteData;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessageType;

public record CommunityChatSendCommand(
        Long postId,
        Long userId,
        String messageKey,
        String message,
        CommunityChatGameInviteData gameInvite,
        CommunityChatMessageType messageType
) {
    public static CommunityChatSendCommand of(
            Long postId, Long userId,
            String messageKey, String message,
            CommunityChatGameInviteData gameInvite,
            CommunityChatMessageType messageType
    ) {
        return new CommunityChatSendCommand(postId, userId, messageKey, message, gameInvite, messageType);
    }
}
