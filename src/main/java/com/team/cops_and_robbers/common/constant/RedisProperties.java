package com.team.cops_and_robbers.common.constant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.data.redis")
public record RedisProperties(
        String host,
        int port,
        String password
) {
    public String getRedissonAddress() {
        return String.format("redis://%s:%d", host, port);
    }
}
