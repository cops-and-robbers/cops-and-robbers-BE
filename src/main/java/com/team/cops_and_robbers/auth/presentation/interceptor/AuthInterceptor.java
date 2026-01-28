package com.team.cops_and_robbers.auth.presentation.interceptor;

import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.util.AuthorizationExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String accessToken = AuthorizationExtractor.extractToken(request)
                .orElseThrow(() -> new ApplicationException(AuthException.UNAUTHENTICATED_REQUEST));

        Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);
        request.setAttribute("loginUser", new LoginUser(userId));

        return true;
    }
}
