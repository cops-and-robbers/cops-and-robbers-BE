package com.team.cops_and_robbers.game.participant.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameParticipantException implements ExceptionCode {

    ALREADY_PARTICIPATING(HttpStatus.BAD_REQUEST, "이미 참가 중인 게임", "이미 해당 게임에 참가하고 있습니다."),
    GAME_FULL(HttpStatus.BAD_REQUEST, "게임 인원 초과", "게임에 참가할 수 있는 최대 인원을 초과했습니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "초대 코드 오류", "유효하지 않거나 일치하지 않는 초대 코드입니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
