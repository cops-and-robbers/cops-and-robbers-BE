package com.team.cops_and_robbers.game.game.application.dto.result;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.util.Arrays;
import java.util.List;

public record GameAreaUpdateResult(GameAreaData areaData) {

    public static GameAreaUpdateResult from(GameArea gameArea) {
        GameAreaData areaData = switch (gameArea.getAreaType()) {
            case CIRCLE -> new GameAreaData.CircleAreaData(
                    gameArea.getPlaygroundCenter().getY(),
                    gameArea.getPlaygroundCenter().getX(),
                    gameArea.getPlaygroundRadiusInMeters(),
                    gameArea.getJailCenter().getY(),
                    gameArea.getJailCenter().getX(),
                    gameArea.getJailRadiusInMeters()
            );
            case POLYGON -> new GameAreaData.PolygonAreaData(
                    toCoordinatesList(gameArea.getPlaygroundPolygon()),
                    toCoordinatesList(gameArea.getJailPolygon())
            );
        };
        return new GameAreaUpdateResult(areaData);
    }

    private static List<Coordinates> toCoordinatesList(Polygon polygon) {
        Coordinate[] coordinates = polygon.getCoordinates();

        return Arrays.stream(coordinates, 0, coordinates.length - 1)
                .map(c -> new Coordinates(c.y, c.x))
                .toList();
    }
}
