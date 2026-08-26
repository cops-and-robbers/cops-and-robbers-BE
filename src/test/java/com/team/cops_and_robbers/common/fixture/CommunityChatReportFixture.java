package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.report.domain.CommunityChatReport;
import com.team.cops_and_robbers.report.domain.ReportType;

public class CommunityChatReportFixture {

    public static CommunityChatReport VERBAL_ABUSE_REPORT(Long chatMessageId, Long reporterUserId, Long reportedUserId) {
        return CommunityChatReport.create(chatMessageId, reporterUserId, reportedUserId, "욕설 메시지", ReportType.VERBAL_ABUSE, null);
    }

    public static CommunityChatReport ETC_REPORT(Long chatMessageId, Long reporterUserId, Long reportedUserId) {
        return CommunityChatReport.create(chatMessageId, reporterUserId, reportedUserId, "기타 메시지", ReportType.ETC, "기타 사유");
    }
}
