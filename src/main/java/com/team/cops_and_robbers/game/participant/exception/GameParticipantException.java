package com.team.cops_and_robbers.game.participant.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameParticipantException implements ExceptionCode {

    ALREADY_PARTICIPATING(HttpStatus.BAD_REQUEST, "이미 참가 중인 게임", "이미 해당 게임에 참가하고 있습니다."),
    GAME_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "이미 시작된 게임", "이미 시작된 게임에는 참여할 수 없습니다."),
    GAME_FULL(HttpStatus.BAD_REQUEST, "게임 인원 초과", "게임에 참가할 수 있는 최대 인원을 초과했습니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "초대 코드 오류", "유효하지 않거나 일치하지 않는 초대 코드입니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참가자를 찾을 수 없음", "해당 게임에 참가하지 않은 사용자입니다."),
    NOT_A_PARTICIPANT(HttpStatus.FORBIDDEN, "참여 권한 없음", "해당 게임의 참가자가 아닙니다."),
    CANNOT_LEAVE_DURING_GAME(HttpStatus.BAD_REQUEST, "게임 진행 중 퇴장 불가", "게임이 시작된 이후에는 방을 나갈 수 없습니다."),
    LOBBY_ACTION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "로비 조작 불가", "게임이 시작된 이후에는 로비 상태를 변경할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
