package com.team.cops_and_robbers.community.presentation.dto.request;

import com.team.cops_and_robbers.community.application.dto.command.CommunityPostUpdateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CommunityPostUpdateRequest(
        @Schema(description = "게시글 제목", example = "같이 경찰과 도둑 하실 분! (수정)")
        @NotBlank(message = "제목은 필수 입력 항목입니다.")
        @Size(max = 100, message = "제목은 최대 100자 입니다.")
        String title,

        @Schema(description = "게시글 내용", example = "강남역 근처에서 6명 모집합니다.")
        @NotBlank(message = "내용은 필수 입력 항목입니다.")
        String content,

        @Schema(description = "모임 날짜/시간", example = "2026-08-10T15:00:00")
        @NotNull(message = "모임 날짜는 필수 입력 항목입니다.")
        LocalDateTime meetingAt,

        @Schema(description = "모임 장소 좌표")
        @NotNull(message = "모임 장소는 필수 입력 항목입니다.")
        @Valid
        Location location,

        @Schema(description = "모집 인원", example = "8")
        @NotNull(message = "모집 인원은 필수 입력 항목입니다.")
        @Min(value = 2, message = "모집 인원은 최소 2명입니다.")
        @Max(value = 50, message = "모집 인원은 최대 50명입니다.")
        Integer maxParticipants
) {
    public record Location(
            @Schema(description = "위도", example = "37.5665")
            @NotNull(message = "위도는 필수 입력 항목입니다.")
            Double latitude,

            @Schema(description = "경도", example = "126.9780")
            @NotNull(message = "경도는 필수 입력 항목입니다.")
            Double longitude
    ) {
    }

    public CommunityPostUpdateCommand toCommand(Long writerId, Long postId) {
        return new CommunityPostUpdateCommand(
                writerId,
                postId,
                title,
                content,
                meetingAt,
                location.latitude(),
                location.longitude(),
                maxParticipants
        );
    }
}
