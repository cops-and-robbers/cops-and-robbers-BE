package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.CommunityNotificationListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CommunityNotificationListResponse(
        @Schema(description = "알림 목록 (최신순). 최근 60일 이내 알림만 내려간다")
        List<CommunityNotificationResponse> content,
        @Schema(description = "다음 페이지 요청에 사용할 커서 (마지막 페이지면 null)", example = "12", nullable = true)
        Long nextCursor,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
    public static CommunityNotificationListResponse from(CommunityNotificationListResult result) {
        return new CommunityNotificationListResponse(
                result.content().stream().map(CommunityNotificationResponse::from).toList(),
                result.nextCursor(),
                result.hasNext()
        );
    }
}
