package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class GameResultFixture {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public static GameResult POLICE_WIN_RESULT(Long gameId) {
        Point center = GEOMETRY_FACTORY.createPoint(new Coordinate(127.0276, 37.4979));
        return GameResult.builder()
                .gameId(gameId)
                .winnerTeam(Team.POLICE)
                .endReason(GameEndReason.ALL_ARRESTED)
                .totalPoliceCount(2)
                .totalRobberCount(3)
                .arrestedRobberCount(3)
                .totalArrestCount(5)
                .durationSeconds(300)
                .playgroundCenter(center)
                .playgroundRadiusInMeters(500)
                .build();
    }
}
