package com.team.cops_and_robbers.community.application.event;

import com.team.cops_and_robbers.community.domain.CommunityChatPin;
import com.team.cops_and_robbers.community.domain.CommunityChatSystemEventType;

public record CommunityChatPinChangedEvent(
        CommunityChatPin pin,
        CommunityChatSystemEventType action,
        Long actorId
) {
}
