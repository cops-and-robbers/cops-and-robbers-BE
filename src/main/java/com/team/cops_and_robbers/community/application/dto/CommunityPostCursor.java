package com.team.cops_and_robbers.community.application.dto;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

public record CommunityPostCursor(
        LocalDateTime createdAt,
        Long id
) {
    private static final String DELIMITER = "\\|";
    private static final int PART_COUNT = 2;

    public static String encode(LocalDateTime createdAt, Long id) {
        String raw = createdAt.truncatedTo(ChronoUnit.MICROS) + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Optional<CommunityPostCursor> decode(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        CommunityPostCursor cursor = parse(value)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_QUERY_PARAMETER));
        return Optional.of(cursor);
    }

    private static Optional<CommunityPostCursor> parse(String value) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
                    .split(DELIMITER);
            if (parts.length != PART_COUNT) {
                return Optional.empty();
            }
            return Optional.of(new CommunityPostCursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1])));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
