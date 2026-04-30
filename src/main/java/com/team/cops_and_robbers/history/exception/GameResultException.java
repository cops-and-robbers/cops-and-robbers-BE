package com.team.cops_and_robbers.history.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameResultException implements ExceptionCode {

    GAME_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "게임 결과를 찾을 수 없음", "해당 게임 결과를 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
