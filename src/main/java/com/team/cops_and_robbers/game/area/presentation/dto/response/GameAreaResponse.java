package com.team.cops_and_robbers.game.area.presentation.dto.response;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.area.application.dto.result.GameAreaResult;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GameAreaResponse(
        @Schema(description = "구역 타입 (CIRCLE | POLYGON)")
        AreaType areaType,

        @Schema(description = "원형 구역 데이터 (CIRCLE 전용)")
        CircleAreaResponse circle,

        @Schema(description = "다각형 구역 데이터 (POLYGON 전용)")
        PolygonAreaResponse polygon
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

    public static GameAreaResponse from(GameAreaResult result) {
        return switch (result.areaData()) {
            case GameAreaData.CircleAreaData c -> fromCircle(c);
            case GameAreaData.PolygonAreaData p -> fromPolygon(p);
        };
    }

    private static GameAreaResponse fromCircle(GameAreaData.CircleAreaData c) {
        return new GameAreaResponse(
                AreaType.CIRCLE,
                new CircleAreaResponse(
                        new Coordinates(c.playgroundLatitude(), c.playgroundLongitude()), c.playgroundRadiusInMeters(),
                        new Coordinates(c.jailLatitude(), c.jailLongitude()), c.jailRadiusInMeters()
                ),
                null
        );
    }

    private static GameAreaResponse fromPolygon(GameAreaData.PolygonAreaData p) {
        return new GameAreaResponse(
                AreaType.POLYGON,
                null,
                new PolygonAreaResponse(p.playgroundPolygon(), p.jailPolygon())
        );
    }
}
