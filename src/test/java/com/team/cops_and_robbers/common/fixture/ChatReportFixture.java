package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.report.domain.ChatReport;
import com.team.cops_and_robbers.report.domain.ReportType;

public class ChatReportFixture {

    public static ChatReport VERBAL_ABUSE_REPORT(Long gameId, Long reporterUserId, Long reportedUserId) {
        return ChatReport.create(gameId, reporterUserId, reportedUserId, "욕설 메시지", ReportType.VERBAL_ABUSE, null);
    }

    public static ChatReport ETC_REPORT(Long gameId, Long reporterUserId, Long reportedUserId) {
        return ChatReport.create(gameId, reporterUserId, reportedUserId, "기타 메시지", ReportType.ETC, "기타 사유");
    }
}
