package com.team.cops_and_robbers.user.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserException implements ExceptionCode {

    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "존재하지 않는 회원", "해당 유저를 찾을 수 없습니다."),
    DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "닉네임 중복", "이미 사용 중인 닉네임입니다. 다른 닉네임을 선택해주세요."),
    CANNOT_WITHDRAW(HttpStatus.CONFLICT, "회원 탈퇴 불가", "진행 중인 게임 세션이 있어 탈퇴할 수 없습니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "필수 약관 미동의", "필수 약관은 모두 동의해야 합니다.")
    ;

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;

}
