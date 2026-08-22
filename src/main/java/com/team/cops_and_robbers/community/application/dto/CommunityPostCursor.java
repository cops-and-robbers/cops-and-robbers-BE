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
 * sortKey는 정렬에 따라 createdAt · meetingAt · 거리(m)이며, 국가나 정렬이 요청과 다르면 거절한다.
 */
public record CommunityPostCursor(
        String countryCode,
        CommunityPostSort sort,
        int closed,
        String sortKey,
        Long id
) {
    private static final String DELIMITER = "\\|";
    private static final int PART_COUNT = 5;

    public static String encode(
            String countryCode, CommunityPostSort sort, boolean closed, String sortKey, Long id) {
        String raw = countryCode + "|" + sort.name() + "|" + (closed ? 1 : 0) + "|" + sortKey + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String sortKeyOf(LocalDateTime sortAt) {
        return sortAt.truncatedTo(ChronoUnit.MICROS).toString();
    }

    public static Optional<CommunityPostCursor> decode(String value, String countryCode, CommunityPostSort sort) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        CommunityPostCursor cursor = parse(value)
                .filter(parsed -> parsed.countryCode().equals(countryCode) && parsed.sort() == sort)
                .orElseThrow(() -> new ApplicationException(CommonException.INVALID_QUERY_PARAMETER));
        return Optional.of(cursor);
    }

    public LocalDateTime sortAt() {
        return LocalDateTime.parse(sortKey);
    }

    public double distance() {
        return Double.parseDouble(sortKey);
    }

    private static void validateSortKey(CommunityPostCursor cursor) {
        if (cursor.sort() == CommunityPostSort.DISTANCE) {
            cursor.distance();
            return;
        }
        cursor.sortAt();
    }

    private static Optional<CommunityPostCursor> parse(String value) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
                    .split(DELIMITER);
            if (parts.length != PART_COUNT) {
                return Optional.empty();
            }
            CommunityPostCursor cursor = new CommunityPostCursor(
                    parts[0],
                    CommunityPostSort.valueOf(parts[1]),
                    Integer.parseInt(parts[2]),
                    parts[3],
                    Long.parseLong(parts[4]));
            validateSortKey(cursor);
            return Optional.of(cursor);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
