package com.team.cops_and_robbers.community.application.dto;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.community.domain.CommunityPostSort;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * 마감 여부가 1차 정렬 기준이라 커서에도 담는다.
 * sortAt은 정렬 기준에 따라 createdAt이거나 meetingAt이며, 커서와 요청의 정렬이 다르면 거절한다.
 */
public record CommunityPostCursor(
        CommunityPostSort sort,
        int closed,
        LocalDateTime sortAt,
        Long id
) {
    private static final String DELIMITER = "\\|";
    private static final int PART_COUNT = 4;

    public static String encode(CommunityPostSort sort, boolean closed, LocalDateTime sortAt, Long id) {
        String raw = sort.name() + "|" + (closed ? 1 : 0) + "|"
                + sortAt.truncatedTo(ChronoUnit.MICROS) + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Optional<CommunityPostCursor> decode(String value, CommunityPostSort sort) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        CommunityPostCursor cursor = parse(value)
                .filter(parsed -> parsed.sort() == sort)
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
                    CommunityPostSort.valueOf(parts[0]),
                    Integer.parseInt(parts[1]),
                    LocalDateTime.parse(parts[2]),
                    Long.parseLong(parts[3])));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return Optional.empty();
        }
    }
}