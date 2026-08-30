package com.team.cops_and_robbers.notice.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 공지 번역이 지원하는 언어.
 * DB에는 enum 이름(KO)이 저장되고, API 요청·응답은 소문자 코드(ko)를 쓴다.
 */
public enum NoticeLanguage {

    KO, JA, EN;

    /** 대소문자를 가리지 않고 받는다. 지원하지 않는 값은 빈 값으로 돌려 호출부의 대체 규칙에 맡긴다. */
    public static Optional<NoticeLanguage> from(String languageCode) {
        return Arrays.stream(values())
                .filter(language -> language.name().equalsIgnoreCase(languageCode))
                .findFirst();
    }

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }
}
