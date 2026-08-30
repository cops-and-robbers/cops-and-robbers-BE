package com.team.cops_and_robbers.notice.application.dto.command;

import com.team.cops_and_robbers.notice.domain.NoticeLanguage;

public record NoticeTranslationCommand(
        NoticeLanguage language,
        String title,
        String content
) {
}
