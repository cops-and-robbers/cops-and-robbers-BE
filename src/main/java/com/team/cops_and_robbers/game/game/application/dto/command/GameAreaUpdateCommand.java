package com.team.cops_and_robbers.game.game.application.dto.command;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.game.presentation.dto.request.CoordinatesRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameAreaRequest;

import java.util.List;

public record GameAreaUpdateCommand(
        Long gameId,
        Long userId,
        GameAreaData areaData
) {
    public static GameAreaUpdateCommand of(Long gameId, Long userId, GameAreaRequest area) {
        return new GameAreaUpdateCommand(gameId, userId, resolveAreaData(area));
    }

    private static GameAreaData resolveAreaData(GameAreaRequest area) {
        if (area.areaType() == null) {
            return new GameAreaData.CircleAreaData(
                    area.playgroundCenter().latitude(),
                    area.playgroundCenter().longitude(),
                    area.playgroundRadiusInMeters(),
                    area.jailCenter().latitude(),
                    area.jailCenter().longitude(),
                    area.jailRadiusInMeters()
            );
        }
        return switch (area.areaType()) {
            case CIRCLE -> new GameAreaData.CircleAreaData(
                    area.circle().playgroundCenter().latitude(),
                    area.circle().playgroundCenter().longitude(),
                    area.circle().playgroundRadiusInMeters(),
                    area.circle().jailCenter().latitude(),
                    area.circle().jailCenter().longitude(),
                    area.circle().jailRadiusInMeters()
            );
            case POLYGON -> new GameAreaData.PolygonAreaData(
                    toCoordinatesList(area.polygon().playgroundPolygon()),
                    toCoordinatesList(area.polygon().jailPolygon())
            );
        };
    }

    private static List<Coordinates> toCoordinatesList(List<CoordinatesRequest> requests) {
        return requests.stream()
                .map(r -> new Coordinates(r.latitude(), r.longitude()))
                .toList();
    }
}
