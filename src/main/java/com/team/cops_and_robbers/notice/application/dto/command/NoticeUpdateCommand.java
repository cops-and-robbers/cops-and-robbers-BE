package com.team.cops_and_robbers.notice.application.dto.command;

import com.team.cops_and_robbers.notice.domain.NoticeCategory;

public record NoticeUpdateCommand(
        Long userId,
        Long noticeId,
        String title,
        String content,
        boolean pinned,
        NoticeCategory category
) {
}
