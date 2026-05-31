package com.team.cops_and_robbers.common.exception;

import org.springframework.http.HttpStatus;

public interface ExceptionCode {
    HttpStatus getHttpStatus();
    String getTitle();
    String getDetail();
    default String getErrorCode() {
        return ((Enum<?>) this).name();
    }
}
