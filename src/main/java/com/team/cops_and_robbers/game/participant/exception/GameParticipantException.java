package com.team.cops_and_robbers.game.participant.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameParticipantException implements ExceptionCode {

    ALREADY_PARTICIPATING(HttpStatus.BAD_REQUEST, "이미 참가 중인 게임", "이미 해당 게임에 참가하고 있습니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
