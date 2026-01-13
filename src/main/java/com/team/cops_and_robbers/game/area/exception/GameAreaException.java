package com.team.cops_and_robbers.game.area.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameAreaException implements ExceptionCode {
    ;

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
