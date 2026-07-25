package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.game.domain.Game;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

public class GameAreaFixture {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // 서울 강남역 근처 좌표
    private static final double DEFAULT_LONGITUDE = 127.0276;
    private static final double DEFAULT_LATITUDE = 37.4979;

    public static GameArea CIRCLE_GAME_AREA(Game game) {
        Point playgroundCenter = createPoint(DEFAULT_LONGITUDE, DEFAULT_LATITUDE);
        Point jailCenter = createPoint(DEFAULT_LONGITUDE + 0.001, DEFAULT_LATITUDE + 0.001);

        return GameArea.builder()
                .game(game)
                .areaType(AreaType.CIRCLE)
                .playgroundCenter(playgroundCenter)
                .playgroundRadiusInMeters(500)
                .jailCenter(jailCenter)
                .jailRadiusInMeters(50)
                .build();
    }

    public static GameArea CIRCLE_GAME_AREA(Game game, double longitude, double latitude) {
        Point playgroundCenter = createPoint(longitude, latitude);
        Point jailCenter = createPoint(longitude + 0.001, latitude + 0.001);

        return GameArea.builder()
                .game(game)
                .areaType(AreaType.CIRCLE)
                .playgroundCenter(playgroundCenter)
                .playgroundRadiusInMeters(500)
                .jailCenter(jailCenter)
                .jailRadiusInMeters(50)
                .build();
    }

    public static GameArea CIRCLE_GAME_AREA(Game game, Point playgroundCenter, Point jailCenter) {
        return GameArea.builder()
                .game(game)
                .areaType(AreaType.CIRCLE)
                .playgroundCenter(playgroundCenter)
                .playgroundRadiusInMeters(500)
                .jailCenter(jailCenter)
                .jailRadiusInMeters(50)
                .build();
    }

    public static GameArea LARGE_CIRCLE_GAME_AREA(Game game) {
        Point playgroundCenter = createPoint(DEFAULT_LONGITUDE, DEFAULT_LATITUDE);
        Point jailCenter = createPoint(DEFAULT_LONGITUDE + 0.002, DEFAULT_LATITUDE + 0.002);

        return GameArea.builder()
                .game(game)
                .areaType(AreaType.CIRCLE)
                .playgroundCenter(playgroundCenter)
                .playgroundRadiusInMeters(1000)
                .jailCenter(jailCenter)
                .jailRadiusInMeters(100)
                .build();
    }

    public static GameArea POLYGON_GAME_AREA(Game game) {
        Polygon playgroundPolygon = createPolygon(new double[][]{
                {127.0276, 37.4979}, {127.0296, 37.4979},
                {127.0296, 37.4999}, {127.0276, 37.4999}
        });
        Polygon jailPolygon = createPolygon(new double[][]{
                {127.0280, 37.4983}, {127.0285, 37.4983},
                {127.0285, 37.4988}, {127.0280, 37.4988}
        });

        return GameArea.builder()
                .game(game)
                .areaType(AreaType.POLYGON)
                .playgroundPolygon(playgroundPolygon)
                .jailPolygon(jailPolygon)
                .build();
    }

    public static Point createPoint(double longitude, double latitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private static Polygon createPolygon(double[][] coords) {
        Coordinate[] coordinates = new Coordinate[coords.length + 1];
        for (int i = 0; i < coords.length; i++) {
            coordinates[i] = new Coordinate(coords[i][0], coords[i][1]);
        }
        coordinates[coords.length] = coordinates[0];
        LinearRing ring = GEOMETRY_FACTORY.createLinearRing(coordinates);
        return GEOMETRY_FACTORY.createPolygon(ring);
    }
}
