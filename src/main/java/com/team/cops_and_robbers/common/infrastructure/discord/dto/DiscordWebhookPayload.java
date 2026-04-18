package com.team.cops_and_robbers.common.infrastructure.discord.dto;

import java.util.List;

public record DiscordWebhookPayload(
        String username,
        List<DiscordEmbed> embeds
) {
    public static DiscordWebhookPayload of(DiscordEmbed embed) {
        return new DiscordWebhookPayload("버그 제보 알림봇", List.of(embed));
    }
}
