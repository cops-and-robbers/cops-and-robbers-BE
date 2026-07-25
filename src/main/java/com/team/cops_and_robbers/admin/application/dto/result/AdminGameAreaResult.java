package com.team.cops_and_robbers.admin.application.dto.result;

import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.util.Arrays;
import java.util.List;

public record AdminGameAreaResult(
        AreaType areaType,
        Double playgroundCenterLat,
        Double playgroundCenterLng,
        Integer playgroundRadiusInMeters,
        Double jailCenterLat,
        Double jailCenterLng,
        Integer jailRadiusInMeters,
        List<AdminCoordinateResult> playgroundPolygon,
        List<AdminCoordinateResult> jailPolygon
) {

    public record AdminCoordinateResult(Double latitude, Double longitude) {}

    public static AdminGameAreaResult from(GameArea gameArea) {
        return switch (gameArea.getAreaType()) {
            case CIRCLE -> new AdminGameAreaResult(
                    AreaType.CIRCLE,
                    gameArea.getPlaygroundCenter().getY(),
                    gameArea.getPlaygroundCenter().getX(),
                    gameArea.getPlaygroundRadiusInMeters(),
                    gameArea.getJailCenter().getY(),
                    gameArea.getJailCenter().getX(),
                    gameArea.getJailRadiusInMeters(),
                    null,
                    null
            );
            case POLYGON -> new AdminGameAreaResult(
                    AreaType.POLYGON,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    toCoordinatesList(gameArea.getPlaygroundPolygon()),
                    toCoordinatesList(gameArea.getJailPolygon())
            );
        };
    }

    private static List<AdminCoordinateResult> toCoordinatesList(Polygon polygon) {
        Coordinate[] coordinates = polygon.getCoordinates();
        return Arrays.stream(coordinates, 0, coordinates.length - 1)
                .map(c -> new AdminCoordinateResult(c.y, c.x))
                .toList();
    }
}
