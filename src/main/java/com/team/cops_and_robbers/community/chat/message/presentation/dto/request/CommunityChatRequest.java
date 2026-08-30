package com.team.cops_and_robbers.community.chat.message.presentation.dto.request;

import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatGameInviteData;
import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.chat.message.application.dto.command.CommunityChatSendCommand;

public record CommunityChatRequest(
        String messageKey,
        String message,
        CommunityChatGameInviteData gameInvite,
        CommunityChatMessageType messageType
) {
    public CommunityChatSendCommand toCommand(Long postId, Long userId) {
        return CommunityChatSendCommand.of(postId, userId, messageKey, message, gameInvite, messageType);
    }
}
