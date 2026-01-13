package com.team.cops_and_robbers.game.area.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameAreaException implements ExceptionCode {

    INVALID_JAIL_RADIUS(HttpStatus.BAD_REQUEST, "유효하지 않은 감옥 반지름", "감옥의 반지름이 운동장의 반지름보다 크거나 같을 수 없습니다."),
    JAIL_OUTSIDE_PLAYGROUND(HttpStatus.BAD_REQUEST, "감옥 영역 벗어남", "감옥은 운동장 내부에 완전히 포함되어야 합니다."),
    GAME_AREA_NOT_FOUND(HttpStatus.NOT_FOUND, "게임 구역을 찾을 수 없음", "해당 게임 구역을 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
