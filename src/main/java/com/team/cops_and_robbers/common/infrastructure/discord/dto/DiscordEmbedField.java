package com.team.cops_and_robbers.common.infrastructure.discord.dto;

import org.springframework.util.StringUtils;

public record DiscordEmbedField(
        String name,
        String value,
        boolean inline
) {
    /**
     * 디스코드 embed field의 value 최대 길이. 초과하거나 비어 있으면 웹훅이 400을 반환해
     * 알림 자체가 전송되지 않으므로 여기서 맞춰 둔다.
     */
    private static final int VALUE_MAX_LENGTH = 1024;
    private static final String EMPTY_VALUE = "-";

    public static DiscordEmbedField of(String name, String value) {
        return new DiscordEmbedField(name, toSafeValue(value), false);
    }

    private static String toSafeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return EMPTY_VALUE;
        }
        if (value.length() <= VALUE_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, VALUE_MAX_LENGTH);
    }
}
