package com.team.cops_and_robbers.notice.application.dto.command;

public record NoticeCreateCommand(
        Long userId,
        String title,
        String content,
        boolean pinned
) {
}