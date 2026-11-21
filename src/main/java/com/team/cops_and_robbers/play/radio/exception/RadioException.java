package com.team.cops_and_robbers.play.radio.exception;

import com.team.cops_and_robbers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RadioException implements ExceptionCode {

    ONLY_CHASER_CAN_USE_RADIO(HttpStatus.FORBIDDEN, "경찰만 무전 가능", "경찰 팀만 무전을 사용할 수 있습니다."),
    RADIO_CHASER_WAITING_TIME(HttpStatus.BAD_REQUEST, "경찰 대기 시간", "경찰은 대기 시간 동안 무전을 사용할 수 없습니다."),
    RADIO_NOT_ALLOWED_IN_EVENT_GAME(HttpStatus.BAD_REQUEST, "이벤트 게임 무전 불가", "이벤트 게임에서는 무전을 사용할 수 없습니다."),
    RADIO_SERVER_BUSY(HttpStatus.SERVICE_UNAVAILABLE, "무전 서버 혼잡", "무전 서버가 혼잡합니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;

    // 옛 앱 호환: 응답 코드는 옛 이름으로 내보낸다. 강제 업데이트 후 제거
    @Override
    public String getErrorCode() {
        return switch (this) {
            case ONLY_CHASER_CAN_USE_RADIO -> "ONLY_POLICE_CAN_USE_RADIO";
            case RADIO_CHASER_WAITING_TIME -> "RADIO_POLICE_WAITING_TIME";
            default -> name();
        };
    }
}
