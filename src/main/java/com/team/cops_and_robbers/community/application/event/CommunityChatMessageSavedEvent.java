package com.team.cops_and_robbers.community.application.event;

import com.team.cops_and_robbers.community.domain.CommunityChatMessage;

public record CommunityChatMessageSavedEvent(CommunityChatMessage message) {
}
