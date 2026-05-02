package com.team.cops_and_robbers.common.infrastructure.discord.dto;

public record DiscordEmbedField(
        String name,
        String value,
        boolean inline
) {
    public static DiscordEmbedField of(String name, String value) {
        return new DiscordEmbedField(name, value, false);
    }
}
