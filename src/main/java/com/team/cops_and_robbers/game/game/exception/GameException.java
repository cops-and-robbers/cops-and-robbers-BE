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
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "게임을 찾을 수 없음", "요청하신 게임 정보가 존재하지 않습니다."),
    INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "초대 코드 생성 실패", "고유한 초대 코드를 생성하지 못했습니다. 잠시 후 다시 시도해주세요."),
    GAME_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "게임 진행 중 아님", "게임이 진행 중인 상태가 아닙니다."),
    GAME_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "비활성 게임", "대기 중이거나 진행 중인 게임에서만 조회할 수 있습니다."),
    GAME_NOT_WAITING(HttpStatus.BAD_REQUEST, "대기 중인 게임이 아님", "대기 중인 게임에서만 설정을 변경할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;
}
