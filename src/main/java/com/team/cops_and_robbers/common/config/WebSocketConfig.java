package com.team.cops_and_robbers.common.config;

import com.team.cops_and_robbers.common.exception.StompExceptionHandler;
import com.team.cops_and_robbers.play.common.StompInterceptor;
import com.team.cops_and_robbers.play.common.StompSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String SOCKET_PATH_PREFIX = "/game-connection";
    private static final String PUBLISH_PATH_PREFIX = "/publish";
    private static final String SUBSCRIBE_PATH_PREFIX = "/subscribe";

    private final StompInterceptor stompInterceptor;
    private final StompExceptionHandler stompExceptionHandler;
    private final StompSessionRegistry stompSessionRegistry;

    /**
     * 1. 메세지 브로커 prefix 설정
     * 클라이언트 -> 서버 : /publish
     * 서버 -> 클라이언트 : /subscribe
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(SUBSCRIBE_PATH_PREFIX);
        registry.setApplicationDestinationPrefixes(PUBLISH_PATH_PREFIX);
    }

    /**
     * 2. 소켓 연결 엔드포인트 설정
     * ws://도메인/ws-game
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.setErrorHandler(stompExceptionHandler)
                .addEndpoint(SOCKET_PATH_PREFIX)
                .setAllowedOriginPatterns("*");
    }

    /**
     * 3. 소켓 통신 인증 인터셉터 등록
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompInterceptor);
    }

    /**
     * 4. WebSocket 세션 추적 데코레이터 등록 (Graceful Shutdown용)
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(stompSessionRegistry);
    }
}
