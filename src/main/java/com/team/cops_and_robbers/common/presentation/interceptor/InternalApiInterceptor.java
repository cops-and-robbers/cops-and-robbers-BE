package com.team.cops_and_robbers.common.presentation.interceptor;

import com.team.cops_and_robbers.common.config.InternalApiProperties;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class InternalApiInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final InternalApiProperties internalApiProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String expected = internalApiProperties.key();
        String actual = request.getHeader(API_KEY_HEADER);

        // 키가 설정되지 않은 환경에서는 모든 내부 요청을 거절한다
        if (!StringUtils.hasText(expected) || actual == null || !matches(expected, actual)) {
            throw new ApplicationException(CommonException.INVALID_INTERNAL_API_KEY);
        }

        return true;
    }

    private boolean matches(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
