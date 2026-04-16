package com.team.cops_and_robbers.common.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum CommonException implements ExceptionCode {

    MISSING_REQUEST_PART(HttpStatus.BAD_REQUEST, "필수 요청 파트 누락", "요청에 필요한 파트가 누락되었습니다."),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "요청 경로를 찾을 수 없음", "요청한 URL에 해당하는 API를 찾을 수 없습니다."),
    INVALID_DESTINATION(HttpStatus.BAD_REQUEST, "잘못된 경로 요청", "요청하신 STOMP 경로가 올바르지 않습니다. 주소를 다시 확인해주세요."),
    INVALID_SOCKET_SESSION(HttpStatus.UNAUTHORIZED, "소켓 연결 오류", "세션 정보를 찾을 수 없습니다. 다시 연결해주세요."),
    UNAUTHORIZED_SUBSCRIPTION(HttpStatus.FORBIDDEN, "구독 권한 없음", "해당 팀 전용 채널을 구독할 권한이 없습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "잘못된 요청 본문", "요청 본문의 형식이 잘못되었습니다."),
    INVALID_QUERY_PARAMETER(HttpStatus.BAD_REQUEST, "잘못된 쿼리 파라미터", "쿼리 파라미터의 형식이 잘못되었습니다."),
    QUERY_PARAMETER_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "쿼리 파라미터 타입 불일치", "요청 파라미터의 타입이 잘못되었습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 입력값", "입력값이 유효성 검사를 통과하지 못했습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입", "서버에서 지원하지 않는 Content-Type 입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 메소드", "해당 엔드 포인트는 서버에서 지원하지 않는 HTTP 메소드 입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류", "서버 내부에 알 수 없는 오류가 발생했습니다. 관리자에게 문의 하세요."),
    FIREBASE_INIT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파이어베이스 SDK 오류", "파이어베이스 SDK 초기화 중 알 수 없는 오류가 발생했습니다."),
    FIREBASE_CONFIG_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "설정 파일 누락", "지정된 경로에서 파이어베이스 서비스 계정 키(JSON)를 찾을 수 없습니다."),
    ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "암호화 실패", "데이터 암호화 중 오류가 발생했습니다."),
    DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "복호화 실패", "데이터 복호화 중 오류가 발생했습니다."),
    INVALID_ENCRYPTION_KEY(HttpStatus.INTERNAL_SERVER_ERROR, "잘못된 암호화 키", "암호화 키는 256bit(32바이트)여야 합니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDetail() {
        return detail;
    }
}
