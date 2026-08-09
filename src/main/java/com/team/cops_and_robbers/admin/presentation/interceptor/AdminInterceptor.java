package com.team.cops_and_robbers.admin.presentation.interceptor;

import com.team.cops_and_robbers.admin.exception.AdminException;
import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.util.AuthorizationExtractor;
import com.team.cops_and_robbers.user.domain.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String accessToken = AuthorizationExtractor.extractToken(request)
                .orElseThrow(() -> new ApplicationException(AdminException.NOT_AUTHENTICATED));

        String role = jwtTokenProvider.getRoleFromAccessToken(accessToken);

        if (!Role.ADMIN.name().equals(role)) {
            throw new ApplicationException(AdminException.FORBIDDEN_ADMIN_ONLY);
        }

        Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);
        request.setAttribute("loginUser", new LoginUser(userId));

        return true;
    }
}
