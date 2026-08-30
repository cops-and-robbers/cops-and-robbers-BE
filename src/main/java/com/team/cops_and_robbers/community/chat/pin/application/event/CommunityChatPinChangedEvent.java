package com.team.cops_and_robbers.community.chat.pin.application.event;

import com.team.cops_and_robbers.community.chat.common.domain.CommunityChatSystemEventType;
import com.team.cops_and_robbers.community.chat.pin.domain.CommunityChatPin;

public record CommunityChatPinChangedEvent(
        CommunityChatPin pin,
        CommunityChatSystemEventType action,
        Long actorId
) {
}
