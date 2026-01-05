package com.team.cops_and_robbers.common.exception;

import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {

    private final ExceptionCode code;

    public ApplicationException(ExceptionCode code) {
        super(code.getDetail());
        this.code = code;
    }
}
