package com.team.cops_and_robbers.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimestampUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 외부 응답용: 현재 시각을 문자열로 반환
    public static String nowKstIso() {
        return OffsetDateTime.now(KST).toString();
    }

    // 외부 응답용: 엔티티 -> 외부 응답 시 문자열로 반환
    public static String toIsoString(LocalDateTime localDateTime) {
        return localDateTime.atZone(KST).toOffsetDateTime().toString();
    }

    // 외부 입력용: ISO 8601 문자열 -> KST 기준 LocalDateTime 변환
    public static LocalDateTime parseToKstLocalDateTime(String isoString) {
        return OffsetDateTime.parse(isoString)
                .atZoneSameInstant(KST)
                .toLocalDateTime();
    }

    // GraphQL DateTime 스칼라용: OffsetDateTime -> KST 기준 LocalDateTime 변환
    public static LocalDateTime toKstLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.atZoneSameInstant(KST).toLocalDateTime();
    }
}
