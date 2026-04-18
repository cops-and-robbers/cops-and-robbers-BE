package com.team.cops_and_robbers.notice.presentation.dto.response;

import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        String title,
        String content,
        boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeResponse from(NoticeResult result) {
        return new NoticeResponse(
                result.id(),
                result.title(),
                result.content(),
                result.pinned(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
