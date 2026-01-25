package com.team.cops_and_robbers.auth.presentation.resolver;

import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.util.AuthorizationExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(LoginUser.class) &&
                parameter.hasParameterAnnotation(AuthUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        LoginUser loginUser = (LoginUser) request.getAttribute("loginUser");    // 앞선 인터셉터에서 이미 파싱했었다면
        if (loginUser != null) {
            return loginUser;
        }

        return getLoginUserFromAccessToken(request);
    }

    private LoginUser getLoginUserFromAccessToken(HttpServletRequest request) {
        String accessToken = AuthorizationExtractor.extractToken(request)
                .orElseThrow(() -> new ApplicationException(AuthException.UNAUTHENTICATED_REQUEST));
        Long loginUserId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);
        return new LoginUser(loginUserId);
    }

}
