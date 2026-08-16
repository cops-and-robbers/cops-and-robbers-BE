package com.team.cops_and_robbers.common.presentation.interceptor;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.presentation.annotation.AllowedQueryParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.Set;

@Component
public class QueryParameterInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AllowedQueryParams allowedQueryParams = findAnnotation(handler);
        if (allowedQueryParams == null) {
            return true;
        }

        validateParameterNames(request, Set.of(allowedQueryParams.value()));

        return true;
    }

    private AllowedQueryParams findAnnotation(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return null;
        }
        return handlerMethod.getMethodAnnotation(AllowedQueryParams.class);
    }

    private void validateParameterNames(HttpServletRequest request, Set<String> allowedNames) {
        for (String name : Collections.list(request.getParameterNames())) {
            if (!allowedNames.contains(name)) {
                throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
            }
        }
    }
}
