package com.team.cops_and_robbers.common.infrastructure.discord;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord.webhook")
public record DiscordProperties(String bug, String alert) {}
