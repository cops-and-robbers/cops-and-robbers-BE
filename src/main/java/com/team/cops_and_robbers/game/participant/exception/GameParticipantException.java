package com.team.cops_and_robbers.game.participant.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameParticipantException implements ExceptionCode {

    ALREADY_PARTICIPATING(HttpStatus.CONFLICT, "이미 참가 중인 게임", "이미 해당 게임에 참가하고 있습니다."),
    GAME_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "이미 시작된 게임", "이미 시작된 게임에는 참여할 수 없습니다."),
    GAME_FULL(HttpStatus.BAD_REQUEST, "게임 인원 초과", "게임에 참가할 수 있는 최대 인원을 초과했습니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "초대 코드 오류", "유효하지 않거나 일치하지 않는 초대 코드입니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참가자를 찾을 수 없음", "해당 게임에 참가하지 않은 사용자입니다."),
    NOT_A_PARTICIPANT(HttpStatus.FORBIDDEN, "참여 권한 없음", "해당 게임의 참가자가 아닙니다."),
    CANNOT_LEAVE_DURING_GAME(HttpStatus.BAD_REQUEST, "게임 진행 중 퇴장 불가", "게임이 시작된 이후에는 방을 나갈 수 없습니다."),
    LOBBY_ACTION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "로비 조작 불가", "게임이 시작된 이후에는 로비 상태를 변경할 수 없습니다."),
    NOT_HOST(HttpStatus.FORBIDDEN, "호스트 권한 필요", "게임을 시작할 수 있는 권한이 없습니다. 방장만 게임을 시작할 수 있습니다."),
    INVALID_TEAM_COMPOSITION(HttpStatus.BAD_REQUEST, "팀 구성 오류", "게임을 시작하려면 경찰과 도둑 팀에 각각 최소 1명 이상의 참가자가 필요합니다."),
    NOT_ALL_READY(HttpStatus.BAD_REQUEST, "준비 미완료", "모든 참가자가 준비 상태여야 게임을 시작할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
