package com.team.cops_and_robbers.common.presentation.interceptor;

import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.util.AuthorizationExtractor;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import com.team.cops_and_robbers.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TermsAgreementInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        Optional<String> accessToken = AuthorizationExtractor.extractToken(request);
        if (accessToken.isEmpty()) {
            return true;
        }

        validateRequiredTermsAgreed(accessToken.get());

        return true;
    }

    private void validateRequiredTermsAgreed(String accessToken) {
        Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);
        User user = userRepository.getByUserId(userId);

        if (!user.hasAgreedRequiredTerms()) {
            throw new ApplicationException(UserException.REQUIRED_TERMS_NOT_AGREED);
        }
    }
}
