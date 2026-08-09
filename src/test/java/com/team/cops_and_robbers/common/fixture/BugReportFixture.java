package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.bug.domain.BugReport;

public class BugReportFixture {

    public static BugReport PENDING_BUG_REPORT(Long userId) {
        return BugReport.builder()
                .content("버그 내용입니다.")
                .userId(userId)
                .build();
    }
}
