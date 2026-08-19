package com.team.cops_and_robbers.common.config;

import com.team.cops_and_robbers.common.exception.StompExceptionHandler;
import com.team.cops_and_robbers.common.presentation.RoutingStompInterceptor;
import com.team.cops_and_robbers.common.presentation.StompAuthInterceptor;
import com.team.cops_and_robbers.play.common.StompSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String SOCKET_PATH_PREFIX = "/connection";
    /**
     * 구버전 앱 호환용. 게임 / 커뮤니티가 같은 소켓을 공유하므로 이름을 /connection으로 바꿨으나,
     * 이미 배포된 앱은 이 경로로 접속하므로 유지한다. 구버전 사용률이 없어지면 제거할 것.
     */
    private static final String LEGACY_SOCKET_PATH_PREFIX = "/game-connection";
    private static final String PUBLISH_PATH_PREFIX = "/publish";
    private static final String SUBSCRIBE_PATH_PREFIX = "/subscribe";

    private final StompAuthInterceptor stompAuthInterceptor;
    private final RoutingStompInterceptor routingStompInterceptor;
    private final StompExceptionHandler stompExceptionHandler;
    private final StompSessionRegistry stompSessionRegistry;

    /**
     * 1. 메세지 브로커 prefix 설정
     * 클라이언트 -> 서버 : /publish
     * 서버 -> 클라이언트 : /subscribe
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();

        registry.enableSimpleBroker(SUBSCRIBE_PATH_PREFIX)
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(scheduler);
        registry.setApplicationDestinationPrefixes(PUBLISH_PATH_PREFIX);
    }

    /**
     * 2. 소켓 연결 엔드포인트 설정
     * ws://도메인/connection
     * 게임과 커뮤니티가 하나의 소켓을 공유하며, 구분은 destination(/publish, /subscribe)으로 한다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.setErrorHandler(stompExceptionHandler)
                .addEndpoint(SOCKET_PATH_PREFIX, LEGACY_SOCKET_PATH_PREFIX)
                .setAllowedOriginPatterns("*");
    }

    /**
     * 3. 소켓 통신 인터셉터 등록 (등록 순서대로 실행)
     * - StompAuthInterceptor: CONNECT 인증 (기능 공통)
     * - RoutingStompInterceptor: destination에 따라 게임 / 커뮤니티 인터셉터로 위임
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor, routingStompInterceptor);
    }

    /**
     * 4. WebSocket 세션 추적 데코레이터 등록 (Graceful Shutdown용)
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(stompSessionRegistry);
    }
}
