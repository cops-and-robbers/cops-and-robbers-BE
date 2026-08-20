package com.team.cops_and_robbers.community.application.dto.result;

import java.util.List;

public record CommunityChatHistoryResult(
        List<CommunityChatMessageResult> messages,
        Long nextCursor,
        boolean hasNext
) {
    public static CommunityChatHistoryResult of(List<CommunityChatMessageResult> messages, boolean hasNext) {
        Long nextCursor = messages.isEmpty() ? null : messages.getLast().id();
        return new CommunityChatHistoryResult(messages, nextCursor, hasNext);
    }
}
