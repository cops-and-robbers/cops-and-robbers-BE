package com.team.cops_and_robbers.common.infrastructure.discord.dto;

import java.util.List;

public record DiscordEmbed(
        String title,
        String description,
        int color,
        List<DiscordEmbedField> fields
) {
    public static DiscordEmbed bug(String description, List<DiscordEmbedField> fields) {
        return new DiscordEmbed("🐛 신규 버그 제보", description, 0xFF6B6B, fields);
    }

    public static DiscordEmbed alert(String description, List<DiscordEmbedField> fields) {
        return new DiscordEmbed("🚨 서버 알림", description, 0xFFA502, fields);
    }
}
