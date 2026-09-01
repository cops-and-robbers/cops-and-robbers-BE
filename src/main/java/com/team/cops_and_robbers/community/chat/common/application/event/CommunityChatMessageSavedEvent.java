package com.team.cops_and_robbers.community.chat.common.application.event;

import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessage;

public record CommunityChatMessageSavedEvent(CommunityChatMessage message) {
}
