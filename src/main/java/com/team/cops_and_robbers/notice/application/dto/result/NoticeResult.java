package com.team.cops_and_robbers.notice.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.notice.domain.Notice;

public record NoticeResult(
        Long id,
        String title,
        String content,
        boolean pinned,
        String createdAt,
        String updatedAt
) {
    public static NoticeResult from(Notice notice) {
        return new NoticeResult(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.isPinned(),
                TimestampUtil.toIsoString(notice.getCreatedAt()),
                TimestampUtil.toIsoString(notice.getUpdatedAt())
        );
    }
}
