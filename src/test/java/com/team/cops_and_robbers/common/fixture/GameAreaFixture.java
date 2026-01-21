package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.game.domain.Game;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class GameAreaFixture {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // 서울 강남역 근처 좌표
    private static final double DEFAULT_LONGITUDE = 127.0276;
    private static final double DEFAULT_LATITUDE = 37.4979;

    public static GameArea GAME_AREA(Game game) {
        Point playgroundCenter = createPoint(DEFAULT_LONGITUDE, DEFAULT_LATITUDE);
        Point jailCenter = createPoint(DEFAULT_LONGITUDE + 0.001, DEFAULT_LATITUDE + 0.001);

        return GameArea.builder()
                .game(game)
                .playgroundCenter(playgroundCenter)
                .playgroundRadiusInMeters(500)
                .jailCenter(jailCenter)
                .jailRadiusInMeters(50)
                .build();
    }

    public static GameArea GAME_AREA(Game game, double longitude, double latitude) {
        Point playgroundCenter = createPoint(longitude, latitude);
        Point jailCenter = createPoint(longitude + 0.001, latitude + 0.001);

        return GameArea.builder()
                .game(game)
                .playgroundCenter(playgroundCenter)
                .playgroundRadiusInMeters(500)
                .jailCenter(jailCenter)
                .jailRadiusInMeters(50)
                .build();
    }

    public static GameArea GAME_AREA(Game game, Point playgroundCenter, Point jailCenter) {
        return GameArea.builder()
                .game(game)
                .playgroundCenter(playgroundCenter)
                .playgroundRadiusInMeters(500)
                .jailCenter(jailCenter)
                .jailRadiusInMeters(50)
                .build();
    }

    public static GameArea LARGE_GAME_AREA(Game game) {
        Point playgroundCenter = createPoint(DEFAULT_LONGITUDE, DEFAULT_LATITUDE);
        Point jailCenter = createPoint(DEFAULT_LONGITUDE + 0.002, DEFAULT_LATITUDE + 0.002);

        return GameArea.builder()
                .game(game)
                .playgroundCenter(playgroundCenter)
                .playgroundRadiusInMeters(1000)
                .jailCenter(jailCenter)
                .jailRadiusInMeters(100)
                .build();
    }

    public static Point createPoint(double longitude, double latitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }
}