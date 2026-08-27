package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.CommunityPostResult;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityPostResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long id,
        @Schema(description = "작성자 ID", example = "1")
        Long writerId,
        @Schema(description = "작성자 닉네임 (탈퇴한 유저면 \"알수없음\")", example = "무서운경찰관")
        String writerNickname,
        @Schema(description = "작성자 프로필 아이콘 번호 (탈퇴한 유저면 기본 아이콘 번호)", example = "1")
        int writerProfileIcon,
        @Schema(description = "제목", example = "같이 경찰과 도둑 하실 분!")
        String title,
        @Schema(description = "내용", example = "강남역 근처에서 5명 모집합니다.")
        String content,
        @Schema(description = "모임 날짜/시간", example = "2026-08-10T14:00:00+09:00")
        String meetingAt,
        @Schema(description = "모임 장소")
        LocationResponse location,
        @Schema(description = "모집 인원", example = "6")
        Integer maxParticipants,
        @Schema(description = "모집 상태. 모임 날짜가 지나면 ENDED", example = "RECRUITING")
        RecruitmentStatus status,
        @Schema(description = "생성일시", example = "2026-08-07T12:00:00+09:00")
        String createdAt,
        @Schema(description = "수정일시", example = "2026-08-07T12:00:00+09:00")
        String updatedAt,
        @Schema(description = "내가 이 게시글의 채팅방에 참여 중인지 여부. 비로그인 조회는 false", example = "false")
        boolean chatJoined,
        @Schema(description = "이 글의 내 알림 설정. 단건 조회에서만 내려가고 목록·비로그인 조회는 null",
                nullable = true)
        CommunityPostNotificationSettingResponse notificationSettings
) {
    public record LocationResponse(
            @Schema(description = "위도", example = "37.4979")
            Double latitude,
            @Schema(description = "경도", example = "127.0276")
            Double longitude,
            @Schema(description = "동 단위 지역 (역지오코딩 실패 시 null)",
                    example = "서울특별시 광진구 군자동", nullable = true)
            String region,
            @Schema(description = "지번 주소. 화면에 노출하지 않고 주소 복사에 쓴다 (역지오코딩 실패 시 null)",
                    example = "서울특별시 광진구 군자동 98", nullable = true)
            String address,
            @Schema(description = "작성자가 입력한 만나는 곳", example = "어린이대공원 정문")
            String placeName,
            @Schema(description = "국가 코드(ISO 3166-1 alpha-2). 역지오코딩 실패 시 null",
                    example = "KR", nullable = true)
            String countryCode
    ) {
    }

    public static CommunityPostResponse from(CommunityPostResult result) {
        return new CommunityPostResponse(
                result.id(),
                result.writerId(),
                result.writerNickname(),
                result.writerProfileIcon(),
                result.title(),
                result.content(),
                result.meetingAt(),
                new LocationResponse(
                        result.location().latitude(),
                        result.location().longitude(),
                        result.location().region(),
                        result.location().address(),
                        result.location().placeName(),
                        result.location().countryCode()),
                result.maxParticipants(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.chatJoined(),
                CommunityPostNotificationSettingResponse.from(result.notificationSettings())
        );
    }
}
