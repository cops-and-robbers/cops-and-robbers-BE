package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.notice.domain.Notice;

public class NoticeFixture {

    public static Notice NOTICE() {
        return Notice.builder()
                .title("공지사항 제목")
                .content("공지사항 내용")
                .pinned(false)
                .build();
    }

    public static Notice PINNED_NOTICE() {
        return Notice.builder()
                .title("고정 공지사항")
                .content("고정된 내용")
                .pinned(true)
                .build();
    }
}
