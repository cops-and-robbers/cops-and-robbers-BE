package com.team.cops_and_robbers.common.config;


import com.team.cops_and_robbers.common.infrastructure.discord.DiscordProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(DiscordProperties.class)
public class AsyncConfig {

    private static final String FCM_NOTIFIER_PREFIX = "fcm-notifier-";

    /**
     * 반환 타입이 {@link ThreadPoolTaskExecutor} 여야 한다.
     * 액추에이터가 {@code TaskExecutor} 타입으로 빈을 찾아 지표를 붙이는데,
     * {@code Executor} 로 선언하면 타입 조회에서 빠져 executor_queued_tasks 가 나오지 않는다.
     */
    @Bean
    public ThreadPoolTaskExecutor fcmExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix(FCM_NOTIFIER_PREFIX);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        ThreadPoolExecutor.DiscardOldestPolicy discardOldest = new ThreadPoolExecutor.DiscardOldestPolicy();
        executor.setRejectedExecutionHandler((task, pool) -> {
            log.warn("[FCM] Executor queue full, discarding oldest task | queued={}, active={}",
                    pool.getQueue().size(), pool.getActiveCount());
            discardOldest.rejectedExecution(task, pool);
        });

        executor.initialize();
        return executor;
    }
}
