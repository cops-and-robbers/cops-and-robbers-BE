package com.team.cops_and_robbers.notice.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import com.team.cops_and_robbers.notice.domain.NoticeLanguage;
import com.team.cops_and_robbers.notice.domain.NoticeTranslation;

public record NoticeResult(
        Long id,
        String title,
        String content,
        String language,
        String requestedLanguage,
        boolean pinned,
        NoticeCategory category,
        String createdAt,
        String updatedAt
) {
    public static NoticeResult from(Notice notice, NoticeTranslation translation, NoticeLanguage requestedLanguage) {
        return new NoticeResult(
                notice.getId(),
                translation.getTitle(),
                translation.getContent(),
                translation.getLanguage().code(),
                requestedLanguage.code(),
                notice.isPinned(),
                notice.getCategory(),
                TimestampUtil.toIsoString(notice.getCreatedAt()),
                TimestampUtil.toIsoString(notice.getUpdatedAt())
        );
    }
}
