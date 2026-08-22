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

/** 마감 여부가 1차 정렬 기준이라 커서에도 담는다. */
public record CommunityPostCursor(
        int closed,
        LocalDateTime createdAt,
        Long id
) {
    private static final String DELIMITER = "\\|";
    private static final int PART_COUNT = 3;

    public static String encode(boolean closed, LocalDateTime createdAt, Long id) {
        String raw = (closed ? 1 : 0) + "|" + createdAt.truncatedTo(ChronoUnit.MICROS) + "|" + id;
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
            return Optional.of(new CommunityPostCursor(
                    Integer.parseInt(parts[0]),
                    LocalDateTime.parse(parts[1]),
                    Long.parseLong(parts[2])));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return Optional.empty();
        }
    }
}