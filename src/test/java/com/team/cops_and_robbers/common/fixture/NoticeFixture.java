package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public class NoticeFixture {

    public static Notice NOTICE() {
        Notice notice = Notice.builder()
                .title("공지사항 제목")
                .content("공지사항 내용")
                .pinned(false)
                .category(NoticeCategory.NOTICE)
                .build();
        ReflectionTestUtils.setField(notice, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(notice, "updatedAt", LocalDateTime.now());
        return notice;
    }

    public static Notice PINNED_NOTICE() {
        Notice notice = Notice.builder()
                .title("고정 공지사항")
                .content("고정된 내용")
                .pinned(true)
                .category(NoticeCategory.NOTICE)
                .build();
        ReflectionTestUtils.setField(notice, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(notice, "updatedAt", LocalDateTime.now());
        return notice;
    }
}
