package com.team.cops_and_robbers.loadtest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

/**
 * 부하테스트 동안 WebSocket 브로커 통계 로깅 주기를 줄인다.
 * <p>
 * WebSocket 세션 수와 인바운드 / 아웃바운드 채널의 큐 적체는 액추에이터에 노출되지 않는다.
 * Gauge 를 새로 등록하는 대신, 이미 있는 {@link WebSocketMessageBrokerStats} 의
 * 로깅 주기만 낮춰서 로그로 관찰한다. 기본값은 30분이라 측정에 쓸 수 없다.
 * <p>
 * {@code WebSocketConfig} 에 넣지 않고 별도 빈으로 뺀 이유:
 * {@code WebSocketMessageBrokerStats} 는 clientInbound / clientOutbound 채널 익스큐터에 의존하고,
 * 그 익스큐터는 다시 {@code WebSocketMessageBrokerConfigurer} 구현체(= WebSocketConfig)를 거쳐 만들어진다.
 * 그래서 WebSocketConfig 가 stats 를 주입받으면 순환 참조가 된다.
 * <p>
 * 프로퍼티로 켜고 끄므로 테스트가 끝나면 플래그만 내리면 되고, 코드를 원복할 필요가 없다.
 */
@Slf4j
@Profile("dev")
@Component
@ConditionalOnProperty(name = "loadtest.stats.logging-period-ms")
public class LoadTestBrokerStatsLogger {

    public LoadTestBrokerStatsLogger(
            WebSocketMessageBrokerStats stats,
            @Value("${loadtest.stats.logging-period-ms}") long loggingPeriodMs
    ) {
        stats.setLoggingPeriod(loggingPeriodMs);
        log.info("[LoadTest] WebSocket 브로커 통계 로깅 주기: {}ms (기본 30분)", loggingPeriodMs);
    }
}
