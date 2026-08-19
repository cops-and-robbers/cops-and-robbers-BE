package com.team.cops_and_robbers.common.presentation;

import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.util.AuthorizationExtractor;
import com.team.cops_and_robbers.common.util.StompSessionHelper;
import com.team.cops_and_robbers.user.exception.UserException;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * 소켓 연결 인증을 담당한다.
 * 게임 / 커뮤니티를 가리지 않고 모든 소켓 연결이 이 경로를 타므로, 특정 기능에 종속된 검증을 넣지 않는다.
 * 여기서 세션에 저장한 userId를 이후 각 기능 인터셉터가 꺼내 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor.getCommand() == StompCommand.CONNECT) {
            handleConnect(accessor);
        }

        return message;
    }

    /**
     * [CONNECT]: 토큰 검증 & 유저 정보 세션 캐싱
     * - web socket 연결한 사용자를 검증하고, 소켓 세션에 유저 정보를 저장합니다.
     * (DB 조회)
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String accessToken = getAccessToken(accessor);
        Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);

        if (!userRepository.existsById(userId)) {
            throw new ApplicationException(UserException.USER_NOT_FOUND);
        }

        StompSessionHelper.putUserId(accessor, userId);
        log.info("[Wed Socket] CONNECT success: userId={}", userId);
    }

    private String getAccessToken(StompHeaderAccessor accessor) {
        return AuthorizationExtractor.extractToken(accessor)
                .orElseThrow(() -> new ApplicationException(AuthException.UNAUTHENTICATED_REQUEST));
    }
}