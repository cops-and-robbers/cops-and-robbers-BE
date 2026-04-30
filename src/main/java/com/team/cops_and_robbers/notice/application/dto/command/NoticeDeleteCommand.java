package com.team.cops_and_robbers.notice.application.dto.command;

public record NoticeDeleteCommand(
        Long userId,
        Long noticeId
) {
}