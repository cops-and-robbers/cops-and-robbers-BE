package com.team.cops_and_robbers.user.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserException implements ExceptionCode {

    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "존재하지 않는 회원", "해당 유저을 찾을 수 없습니다."),
    DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "닉네임 중복", "이미 사용 중인 닉네임입니다. 다른 닉네임을 선택해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;

}
