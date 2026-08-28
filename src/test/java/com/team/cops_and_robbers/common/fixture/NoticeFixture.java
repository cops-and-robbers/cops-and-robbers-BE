package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import com.team.cops_and_robbers.notice.domain.NoticeLanguage;
import com.team.cops_and_robbers.notice.domain.NoticeTranslation;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public class NoticeFixture {

    public static Notice NOTICE() {
        Notice notice = Notice.builder()
                .pinned(false)
                .category(NoticeCategory.NOTICE)
                .originalLanguage(NoticeLanguage.KO)
                .build();
        setTimestamps(notice);
        return notice;
    }

    public static Notice PINNED_NOTICE() {
        Notice notice = Notice.builder()
                .pinned(true)
                .category(NoticeCategory.NOTICE)
                .originalLanguage(NoticeLanguage.KO)
                .build();
        setTimestamps(notice);
        return notice;
    }

    public static Notice MAINTENANCE_NOTICE() {
        Notice notice = Notice.builder()
                .pinned(false)
                .category(NoticeCategory.MAINTENANCE)
                .originalLanguage(NoticeLanguage.KO)
                .build();
        setTimestamps(notice);
        return notice;
    }

    public static NoticeTranslation KO_TRANSLATION(Long noticeId) {
        NoticeTranslation translation = NoticeTranslation.builder()
                .noticeId(noticeId)
                .language(NoticeLanguage.KO)
                .title("공지사항 제목")
                .content("공지사항 내용")
                .build();
        setTimestamps(translation);
        return translation;
    }

    public static NoticeTranslation JA_TRANSLATION(Long noticeId) {
        NoticeTranslation translation = NoticeTranslation.builder()
                .noticeId(noticeId)
                .language(NoticeLanguage.JA)
                .title("お知らせのタイトル")
                .content("お知らせの内容")
                .build();
        setTimestamps(translation);
        return translation;
    }

    private static void setTimestamps(Object entity) {
        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(entity, "updatedAt", LocalDateTime.now());
    }
}
