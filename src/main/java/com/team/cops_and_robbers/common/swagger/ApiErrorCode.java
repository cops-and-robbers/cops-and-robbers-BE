package com.team.cops_and_robbers.common.swagger;

import com.team.cops_and_robbers.common.exception.ExceptionCode;

import java.lang.annotation.*;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiErrorCodes.class)
public @interface ApiErrorCode {
    /**
     * 에러 코드가 정의된 Enum 클래스
     */
    Class<? extends ExceptionCode> value();

    /**
     * 문서화할 에러 코드 상수 이름들
     */
    String[] codes() default {};
}
