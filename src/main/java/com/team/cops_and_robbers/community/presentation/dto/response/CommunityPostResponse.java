package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityPostResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long id,
        @Schema(description = "작성자 ID", example = "1")
        Long writerId,
        @Schema(description = "제목", example = "같이 경찰과 도둑 하실 분!")
        String title,
        @Schema(description = "내용", example = "강남역 근처에서 5명 모집합니다.")
        String content,
        @Schema(description = "모임 날짜/시간", example = "2026-08-10T14:00:00+09:00")
        String meetingAt,
        @Schema(description = "모임 장소 좌표")
        LocationResponse location,
        @Schema(description = "모집 인원", example = "6")
        Integer maxParticipants,
        @Schema(description = "모집 상태", example = "RECRUITING")
        RecruitmentStatus status,
        @Schema(description = "생성일시", example = "2026-08-07T12:00:00+09:00")
        String createdAt,
        @Schema(description = "수정일시", example = "2026-08-07T12:00:00+09:00")
        String updatedAt
) {
    public record LocationResponse(
            @Schema(description = "위도", example = "37.4979")
            Double latitude,
            @Schema(description = "경도", example = "127.0276")
            Double longitude
    ) {
    }

    public static CommunityPostResponse from(CommunityPostResult result) {
        return new CommunityPostResponse(
                result.id(),
                result.writerId(),
                result.title(),
                result.content(),
                result.meetingAt(),
                new LocationResponse(result.location().latitude(), result.location().longitude()),
                result.maxParticipants(),
                result.status(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
