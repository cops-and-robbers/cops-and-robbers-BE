package com.team.cops_and_robbers.community.chat.common.domain;

public record CommunityChatSystemEventData(
        CommunityChatSystemEventType event
) {
    public static CommunityChatSystemEventData of(CommunityChatSystemEventType event) {
        return new CommunityChatSystemEventData(event);
    }
}