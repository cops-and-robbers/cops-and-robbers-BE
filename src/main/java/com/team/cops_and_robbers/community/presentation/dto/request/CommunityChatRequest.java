package com.team.cops_and_robbers.community.presentation.dto.request;

import com.team.cops_and_robbers.community.application.dto.command.CommunityChatSendCommand;
import com.team.cops_and_robbers.community.domain.CommunityChatGameInviteData;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;

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
