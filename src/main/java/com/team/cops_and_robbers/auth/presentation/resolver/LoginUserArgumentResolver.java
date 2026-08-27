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

import java.util.Optional;

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

        boolean required = parameter.getParameterAnnotation(AuthUser.class).required();
        return getLoginUserFromAccessToken(request, required);
    }

    /**
     * required가 false면 토큰이 아예 없을 때만 null을 반환한다.
     * 토큰이 있는데 유효하지 않으면 required 여부와 무관하게 그대로 예외를 던진다.
     */
    private LoginUser getLoginUserFromAccessToken(HttpServletRequest request, boolean required) {
        Optional<String> accessToken = AuthorizationExtractor.extractToken(request);
        if (accessToken.isEmpty()) {
            if (required) {
                throw new ApplicationException(AuthException.UNAUTHENTICATED_REQUEST);
            }
            return null;
        }
        Long loginUserId = jwtTokenProvider.getUserIdFromAccessToken(accessToken.get());
        return new LoginUser(loginUserId);
    }

}
