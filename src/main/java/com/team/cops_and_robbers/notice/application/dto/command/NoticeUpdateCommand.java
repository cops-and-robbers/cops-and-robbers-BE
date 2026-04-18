package com.team.cops_and_robbers.notice.application.dto.command;

public record NoticeUpdateCommand(
        Long noticeId,
        String title,
        String content,
        boolean pinned
) {
}
