package com.team.cops_and_robbers.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
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

    // 내부 로직용: 엔티티 저장 시 현재 시간 반환
    public static LocalDateTime nowKstLocal() {
        return LocalDateTime.now(KST);
    }

    // 내부 로직용: 스케줄러 작업 예약 시 Instant 변환
    public static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(KST).toInstant();
    }
}
