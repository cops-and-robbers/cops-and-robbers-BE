package com.team.cops_and_robbers.notice.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NoticeException implements ExceptionCode {

    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없음", "해당 공지사항을 찾을 수 없습니다."),
    FORBIDDEN_ADMIN_ONLY(HttpStatus.FORBIDDEN, "권한 없음", "관리자 권한이 필요합니다."),
    TRANSLATION_NOT_FOUND(HttpStatus.NOT_FOUND, "번역 없음", "해당 공지사항의 번역을 찾을 수 없습니다."),
    DUPLICATE_TRANSLATION_LANGUAGE(HttpStatus.BAD_REQUEST, "언어 중복", "같은 언어의 번역이 두 번 들어왔습니다."),
    MISSING_ORIGINAL_TRANSLATION(HttpStatus.BAD_REQUEST, "원문 번역 누락", "원문 언어의 번역이 포함되어야 합니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;

}
