package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.report.domain.CommunityPostReport;
import com.team.cops_and_robbers.report.domain.ReportType;

public class CommunityPostReportFixture {

    public static CommunityPostReport SPAM_REPORT(Long postId, Long reporterUserId, Long reportedUserId) {
        return CommunityPostReport.create(postId, "같이 경찰과 도둑 하실 분!", "강남역 근처에서 5명 모집합니다.",
                reporterUserId, reportedUserId, ReportType.SPAM, null);
    }

    public static CommunityPostReport ETC_REPORT(Long postId, Long reporterUserId, Long reportedUserId) {
        return CommunityPostReport.create(postId, "같이 경찰과 도둑 하실 분!", "강남역 근처에서 5명 모집합니다.",
                reporterUserId, reportedUserId, ReportType.ETC, "기타 사유");
    }
}
