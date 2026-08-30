package com.team.cops_and_robbers.notice.application.dto.command;

import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import com.team.cops_and_robbers.notice.domain.NoticeLanguage;

import java.util.List;

public record NoticeCreateCommand(
        Long userId,
        boolean pinned,
        NoticeCategory category,
        NoticeLanguage originalLanguage,
        List<NoticeTranslationCommand> translations
) {
}
