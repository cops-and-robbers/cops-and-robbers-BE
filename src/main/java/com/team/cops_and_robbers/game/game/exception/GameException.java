package com.team.cops_and_robbers.game.game.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameException implements ExceptionCode {

    INVALID_LOCATION_INTERVAL(HttpStatus.BAD_REQUEST, "유효하지 않은 위치 공개 주기", "위치 공개 주기는 라운드 시간보다 짧아야 합니다."),
    INVALID_POLICE_WAIT_TIME(HttpStatus.BAD_REQUEST, "유효하지 않은 경찰 대기 시간", "경찰 대기 시간은 라운드 시간보다 짧아야 합니다."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "게임을 찾을 수 없음", "요청하신 게임 정보가 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
