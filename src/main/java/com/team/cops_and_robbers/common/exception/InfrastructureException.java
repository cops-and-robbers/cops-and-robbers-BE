package com.team.cops_and_robbers.common.exception;

import lombok.Getter;

@Getter
public class InfrastructureException extends RuntimeException {

    private final ExceptionCode code;

    public InfrastructureException(ExceptionCode code) {
        super(code.getDetail());
        this.code = code;
    }
}
