package com.team.cops_and_robbers.notice.application.dto.result;

import com.team.cops_and_robbers.notice.domain.Notice;

import java.time.LocalDateTime;

public record NoticeResult(
        Long id,
        String title,
        String content,
        boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoticeResult from(Notice notice) {
        return new NoticeResult(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.isPinned(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
