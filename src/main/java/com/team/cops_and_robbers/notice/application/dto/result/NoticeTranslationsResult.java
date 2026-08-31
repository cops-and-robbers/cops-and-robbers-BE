package com.team.cops_and_robbers.notice.application.dto.result;

import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.domain.NoticeTranslation;

import java.util.List;

public record NoticeTranslationsResult(
        Long noticeId,
        String originalLanguage,
        List<TranslationResult> translations
) {
    public record TranslationResult(
            String language,
            String title,
            String content
    ) {
        public static TranslationResult from(NoticeTranslation translation) {
            return new TranslationResult(
                    translation.getLanguage().code(),
                    translation.getTitle(),
                    translation.getContent()
            );
        }
    }

    public static NoticeTranslationsResult from(Notice notice, List<NoticeTranslation> translations) {
        return new NoticeTranslationsResult(
                notice.getId(),
                notice.getOriginalLanguage().code(),
                translations.stream().map(TranslationResult::from).toList()
        );
    }
}
