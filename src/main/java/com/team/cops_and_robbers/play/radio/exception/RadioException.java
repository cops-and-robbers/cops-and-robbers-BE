package com.team.cops_and_robbers.play.radio.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RadioException implements ExceptionCode {

    ONLY_POLICE_CAN_USE_RADIO(HttpStatus.FORBIDDEN, "경찰만 무전 가능", "경찰 팀만 무전을 사용할 수 있습니다."),
    RADIO_POLICE_WAITING_TIME(HttpStatus.BAD_REQUEST, "경찰 대기 시간", "경찰은 대기 시간 동안 무전을 사용할 수 없습니다."),
    RADIO_NOT_ALLOWED_IN_EVENT_GAME(HttpStatus.BAD_REQUEST, "이벤트 게임 무전 불가", "이벤트 게임에서는 무전을 사용할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
