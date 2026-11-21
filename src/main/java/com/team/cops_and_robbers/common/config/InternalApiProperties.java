package com.team.cops_and_robbers.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-api")
public record InternalApiProperties(String key) {}
