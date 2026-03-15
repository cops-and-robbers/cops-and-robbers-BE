package com.team.cops_and_robbers.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 제목", example = "유효하지 않은 입력값")
        String title,
        @Schema(description = "HTTP 상태 코드", example = "400")
        int status,
        @Schema(description = "에러 상세 내용", example = "입력값이 유효성 검사를 통과하지 못했습니다.")
        String detail,
        @Schema(description = "에러 발생 URI", example = "/api/games/1")
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
