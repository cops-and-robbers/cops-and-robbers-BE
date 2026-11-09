package com.team.cops_and_robbers.game.game.application.dto.command;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.game.domain.GameMode;
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
        Integer maxParticipants,
        Integer shrinkFinalRadiusInMeters,
        GameMode gameMode
) {

    public GameCreateCommand {
        if (locationRevealIntervalMinutes >= roundDurationMinutes) {
            throw new ApplicationException(GameException.INVALID_LOCATION_INTERVAL);
        }
        if (policeWaitMinutes >= roundDurationMinutes) {
            throw new ApplicationException(GameException.INVALID_POLICE_WAIT_TIME);
        }
        if (gameMode == null) {
            gameMode = GameMode.COPS_AND_ROBBERS;
        }
    }

    public static GameCreateCommand of(
            Long hostUserId,
            GameAreaRequest area,
            GameSettingsRequest settings,
            GameMode gameMode
    ) {
        return new GameCreateCommand(
                hostUserId,
                resolveAreaData(area),
                settings.roundDurationMinutes(),
                settings.locationRevealIntervalMinutes(),
                settings.policeWaitMinutes(),
                settings.maxParticipants(),
                settings.shrinkFinalRadiusInMeters(),
                gameMode
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
                    area.circle().jailCenter() == null ? null : area.circle().jailCenter().latitude(),
                    area.circle().jailCenter() == null ? null : area.circle().jailCenter().longitude(),
                    area.circle().jailRadiusInMeters()
            );
            case POLYGON -> new GameAreaData.PolygonAreaData(
                    toCoordinatesList(area.polygon().playgroundPolygon()),
                    area.polygon().jailPolygon() == null ? null : toCoordinatesList(area.polygon().jailPolygon())
            );
        };
    }

    private static List<Coordinates> toCoordinatesList(List<CoordinatesRequest> requests) {
        return requests.stream()
                .map(r -> new Coordinates(r.latitude(), r.longitude()))
                .toList();
    }
}
