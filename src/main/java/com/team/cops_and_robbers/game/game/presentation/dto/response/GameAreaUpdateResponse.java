package com.team.cops_and_robbers.game.game.presentation.dto.response;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.game.application.dto.result.GameAreaUpdateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GameAreaUpdateResponse(
        @Schema(description = "구역 타입 (CIRCLE | POLYGON)")
        AreaType areaType,

        @Schema(description = "원형 구역 데이터 (CIRCLE 전용)")
        CircleAreaResponse circle,

        @Schema(description = "다각형 구역 데이터 (POLYGON 전용)")
        PolygonAreaResponse polygon,

        // Deprecated: 구버전 앱(v2.12.0) 하위호환용. CIRCLE일 때만 값이 있음
        Coordinates playgroundCenter,
        Integer playgroundRadiusInMeters,
        Coordinates jailCenter,
        Integer jailRadiusInMeters
) {
    public record CircleAreaResponse(
            @Schema(description = "플레이그라운드 중심 좌표")
            Coordinates playgroundCenter,
            @Schema(description = "플레이그라운드 반경(m)", example = "1000")
            Integer playgroundRadiusInMeters,
            @Schema(description = "감옥 중심 좌표")
            Coordinates jailCenter,
            @Schema(description = "감옥 반경(m)", example = "100")
            Integer jailRadiusInMeters
    ) {}

    public record PolygonAreaResponse(
            @Schema(description = "플레이그라운드 꼭짓점 좌표 목록")
            List<Coordinates> playgroundPolygon,
            @Schema(description = "감옥 꼭짓점 좌표 목록")
            List<Coordinates> jailPolygon
    ) {}

    public static GameAreaUpdateResponse from(GameAreaUpdateResult result) {
        return switch (result.areaData()) {
            case GameAreaData.CircleAreaData c -> fromCircle(c);
            case GameAreaData.PolygonAreaData p -> fromPolygon(p);
        };
    }

    private static GameAreaUpdateResponse fromCircle(GameAreaData.CircleAreaData c) {
        Coordinates pgCenter = new Coordinates(c.playgroundLatitude(), c.playgroundLongitude());
        Coordinates jCenter  = new Coordinates(c.jailLatitude(), c.jailLongitude());
        return new GameAreaUpdateResponse(
                AreaType.CIRCLE,
                new CircleAreaResponse(pgCenter, c.playgroundRadiusInMeters(), jCenter, c.jailRadiusInMeters()),
                null,
                pgCenter, c.playgroundRadiusInMeters(),
                jCenter,  c.jailRadiusInMeters()
        );
    }

    private static GameAreaUpdateResponse fromPolygon(GameAreaData.PolygonAreaData p) {
        return new GameAreaUpdateResponse(
                AreaType.POLYGON,
                null,
                new PolygonAreaResponse(p.playgroundPolygon(), p.jailPolygon()),
                null, null, null, null
        );
    }
}
