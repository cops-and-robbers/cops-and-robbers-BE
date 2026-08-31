package com.team.cops_and_robbers.admin.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminException implements ExceptionCode {

    NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", "로그인 후 이용해주세요."),
    FORBIDDEN_ADMIN_ONLY(HttpStatus.FORBIDDEN, "어드민 권한 필요", "어드민 계정만 접근할 수 있습니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "잘못된 날짜 형식", "날짜 형식이 올바르지 않습니다. ISO 8601 형식으로 입력해주세요."),
    EMPTY_TERMS_RESET_TARGET(HttpStatus.BAD_REQUEST, "초기화할 약관 미지정", "초기화할 약관을 하나 이상 선택해주세요.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
