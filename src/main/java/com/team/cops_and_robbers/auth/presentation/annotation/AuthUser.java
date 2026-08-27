package com.team.cops_and_robbers.auth.presentation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {

    /**
     * false면 토큰이 없어도 401을 던지지 않고 loginUser에 null을 주입
     * 이는 웹사이트에서 상세 조회 목록은 일부 정보를 로그인하지 않고도 보여주기 위해 도입
     */
    boolean required() default true;
}
