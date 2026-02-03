package com.team.cops_and_robbers.auth.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthException implements ExceptionCode {

    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "로그인 실패", "소셜 로그인에 실패하였습니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 만료", "인증 정보가 만료되었습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인 만료", "로그인이 만료되었습니다. 다시 로그인해주세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보", "인증 정보가 올바르지 않습니다. 다시 로그인해주세요."),
    UNAUTHENTICATED_REQUEST(HttpStatus.UNAUTHORIZED, "인증되지 않은 요청", "로그인이 필요합니다."),
    UNSUPPORTED_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식", "지원하지 않는 소셜 로그인 방식입니다."),
    FORBIDDEN_ADMIN_ONLY(HttpStatus.FORBIDDEN, "권한 없음", "관리자 권한이 필요합니다."),
    NICKNAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "회원가입 실패", "랜덤 닉네임 생성에 실패했습니다. 잠시 후 다시 시도해주세요."),
    EXPIRED_FIREBASE_TOKEN(HttpStatus.UNAUTHORIZED, "Firebase 인증 만료", "Firebase 토큰이 만료되었습니다. 다시 인증해주세요."),
    INVALID_FIREBASE_TOKEN(HttpStatus.UNAUTHORIZED, "Firebase 인증 실패", "유효하지 않은 Firebase 토큰입니다."),
    FIREBASE_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Firebase 통신 오류", "Firebase 서버 통신 중 알 수 없는 오류가 발생했습니다.");
    ;

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;

}
