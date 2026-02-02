package com.team.cops_and_robbers.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimestampUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static String nowKstIso() {
        return OffsetDateTime.now(KST).toString();
    }

    public static ZonedDateTime nowKstZoned() {
        return ZonedDateTime.now(KST);
    }
}
