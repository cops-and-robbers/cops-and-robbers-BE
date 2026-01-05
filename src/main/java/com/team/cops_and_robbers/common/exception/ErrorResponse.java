package com.team.cops_and_robbers.common.exception;

public record ErrorResponse(
        String title,
        int status,
        String detail,
        String instance
) {
    public static ErrorResponse of(ExceptionCode code, String instance) {
        return new ErrorResponse(code.getTitle(), code.getHttpStatus().value(), code.getDetail(), instance);
    }

    // 동적인 상세 메시지를 직접 응답에 지정
    public static ErrorResponse of(ExceptionCode code, String detail, String instance) {
        return new ErrorResponse(code.getTitle(), code.getHttpStatus().value(), detail, instance);
    }
}
