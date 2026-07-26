package com.team.cops_and_robbers.game.game.application.dto.command;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.presentation.dto.request.CoordinatesRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameAreaRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameSettingsRequest;

import java.util.List;

public record GameCreateCommand(
        Long hostUserId,
        GameAreaData areaData,
        Integer roundDurationMinutes,
        Integer locationRevealIntervalMinutes,
        Integer policeWaitMinutes,
        Integer maxParticipants
) {

    public GameCreateCommand {
        if (locationRevealIntervalMinutes >= roundDurationMinutes) {
            throw new ApplicationException(GameException.INVALID_LOCATION_INTERVAL);
        }
        if (policeWaitMinutes >= roundDurationMinutes) {
            throw new ApplicationException(GameException.INVALID_POLICE_WAIT_TIME);
        }
    }

    public static GameCreateCommand of(Long hostUserId, GameAreaRequest area, GameSettingsRequest settings) {
        return new GameCreateCommand(
                hostUserId,
                resolveAreaData(area),
                settings.roundDurationMinutes(),
                settings.locationRevealIntervalMinutes(),
                settings.policeWaitMinutes(),
                settings.maxParticipants()
        );
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
