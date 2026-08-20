package com.team.cops_and_robbers.community.application.dto.command;

import com.team.cops_and_robbers.community.domain.CommunityChatGameInviteData;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;

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
