package com.team.cops_and_robbers.report.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportType {

    FISHING("낚시/놀람/도배"),
    VERBAL_ABUSE("욕설/비하"),
    IMPERSONATION("사칭/사기"),
    SPAM("광고/스팸"),
    CHEATING("부정 행위/버그 악용"),
    DEMORALIZATION("팀 사기 저하"),
    ETC("기타");

    private final String description;
}
