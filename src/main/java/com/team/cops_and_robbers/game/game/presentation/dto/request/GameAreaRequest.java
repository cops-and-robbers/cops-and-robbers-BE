package com.team.cops_and_robbers.game.game.presentation.dto.request;

import com.team.cops_and_robbers.game.area.domain.AreaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GameAreaRequest(
        @Schema(description = "구역 타입 (CIRCLE | POLYGON)")
        AreaType areaType,

        @Schema(description = "원형 구역 데이터 (CIRCLE 전용)")
        @Valid
        CircleAreaRequest circle,

        @Schema(description = "다각형 구역 데이터 (POLYGON 전용)")
        @Valid
        PolygonAreaRequest polygon,

        // Deprecated: 구버전 앱(v2.12.0) 하위호환용 / areaType == null 일 때만 유효
        @Valid CoordinatesRequest playgroundCenter,
        Integer playgroundRadiusInMeters,
        @Valid CoordinatesRequest jailCenter,
        Integer jailRadiusInMeters
) {

    public GameAreaRequest(AreaType areaType, CircleAreaRequest circle, PolygonAreaRequest polygon) {
        this(areaType, circle, polygon, null, null, null, null);
    }

    @AssertTrue(message = "구역 타입에 맞는 데이터를 입력해주세요.")
    public boolean isAreaDataConsistent() {
        if (areaType == null) return true;

        return switch (areaType) {
            case CIRCLE -> circle != null;
            case POLYGON -> polygon != null;
        };
    }

    @AssertTrue(message = "구버전 요청의 구역 데이터가 올바르지 않습니다.")
    public boolean isLegacyAreaDataValid() {
        if (areaType != null) return true;
        if (playgroundCenter == null) return false;
        if (playgroundRadiusInMeters == null || playgroundRadiusInMeters < 10) return false;
        if (jailCenter == null) return false;
        if (jailRadiusInMeters == null || jailRadiusInMeters < 5) return false;
        return true;
    }

    public record CircleAreaRequest(
            @Schema(description = "플레이그라운드 중심 좌표")
            @Valid @NotNull(message = "플레이그라운드 중심 좌표를 입력해주세요.")
            CoordinatesRequest playgroundCenter,

            @Schema(description = "플레이그라운드 반경(m)", example = "1000")
            @NotNull(message = "플레이그라운드 반경을 입력해주세요.")
            @Min(value = 10, message = "반경은 최소 10m 이상이어야 합니다.")
            Integer playgroundRadiusInMeters,

            @Schema(description = "감옥 중심 좌표")
            @Valid @NotNull(message = "감옥 중심 좌표를 입력해주세요.")
            CoordinatesRequest jailCenter,

            @Schema(description = "감옥 반경(m)", example = "100")
            @NotNull(message = "감옥 반경을 입력해주세요.")
            @Min(value = 5, message = "감옥 반경은 최소 5m 이상이어야 합니다.")
            Integer jailRadiusInMeters
    ) {}

    public record PolygonAreaRequest(
            @Schema(description = "플레이그라운드 꼭짓점 좌표 목록 (최소 3개)")
            @Valid @NotNull(message = "플레이그라운드 꼭짓점 좌표를 입력해주세요.")
            @Size(min = 3, message = "플레이그라운드 꼭짓점은 최소 3개 이상이어야 합니다.")
            List<CoordinatesRequest> playgroundPolygon,

            @Schema(description = "감옥 꼭짓점 좌표 목록 (최소 3개)")
            @Valid @NotNull(message = "감옥 꼭짓점 좌표를 입력해주세요.")
            @Size(min = 3, message = "감옥 꼭짓점은 최소 3개 이상이어야 합니다.")
            List<CoordinatesRequest> jailPolygon
    ) {}
}
