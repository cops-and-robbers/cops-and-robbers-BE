package com.team.cops_and_robbers.admin.application.dto.result.game;

import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.history.domain.GameResult;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

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

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final int MIN_POLYGON_VERTEX_COUNT = 3;

    public record AdminCoordinateResult(Double latitude, Double longitude) {}

    public static AdminGameAreaResult from(GameArea gameArea) {
        return switch (gameArea.getAreaType()) {
            case CIRCLE -> new AdminGameAreaResult(
                    AreaType.CIRCLE,
                    gameArea.getPlaygroundCenter().getY(),
                    gameArea.getPlaygroundCenter().getX(),
                    gameArea.getPlaygroundRadiusInMeters(),
                    gameArea.getJailCenter() == null ? null : gameArea.getJailCenter().getY(),
                    gameArea.getJailCenter() == null ? null : gameArea.getJailCenter().getX(),
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
                    gameArea.getJailPolygon() == null ? null : toCoordinatesList(gameArea.getJailPolygon())
            );
        };
    }

    public static AdminGameAreaResult from(GameResult gameResult) {
        return switch (gameResult.getAreaType()) {
            case CIRCLE -> new AdminGameAreaResult(
                    AreaType.CIRCLE,
                    gameResult.getPlaygroundCenter().getY(),
                    gameResult.getPlaygroundCenter().getX(),
                    gameResult.getPlaygroundRadiusInMeters(),
                    gameResult.getJailCenter() == null ? null : gameResult.getJailCenter().getY(),
                    gameResult.getJailCenter() == null ? null : gameResult.getJailCenter().getX(),
                    gameResult.getJailRadiusInMeters(),
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
                    toCoordinatesList(gameResult.getPlaygroundPolygon()),
                    gameResult.getJailPolygon() == null ? null : toCoordinatesList(gameResult.getJailPolygon())
            );
        };
    }

    /**
     * 동네 이름을 찾을 때 기준으로 쓰는 지점입니다. 원형은 중심, 다각형은 무게중심을 씁니다.
     * 다각형은 꼭짓점 평균을 쓰면 꼭짓점이 한쪽에 몰린 구역에서 기준이 그쪽으로 치우칩니다.
     * ㄷ자처럼 오목한 구역은 무게중심이 구역 밖에 떨어질 수 있어, 그때는 구역 안의 한 점을 씁니다.
     *
     * @return 좌표가 비어 있는 기록이면 null
     */
    public AdminCoordinateResult representativePoint() {
        if (areaType == AreaType.CIRCLE) {
            if (playgroundCenterLat == null || playgroundCenterLng == null) {
                return null;
            }
            return new AdminCoordinateResult(playgroundCenterLat, playgroundCenterLng);
        }

        if (playgroundPolygon == null || playgroundPolygon.size() < MIN_POLYGON_VERTEX_COUNT) {
            return null;
        }
        Polygon polygon = toPolygon(playgroundPolygon);
        Point centroid = polygon.getCentroid();
        Point point = polygon.contains(centroid) ? centroid : polygon.getInteriorPoint();
        return new AdminCoordinateResult(point.getY(), point.getX());
    }

    private static Polygon toPolygon(List<AdminCoordinateResult> vertices) {
        Coordinate[] ring = new Coordinate[vertices.size() + 1];
        for (int i = 0; i < vertices.size(); i++) {
            ring[i] = new Coordinate(vertices.get(i).longitude(), vertices.get(i).latitude());
        }
        ring[vertices.size()] = ring[0];
        return GEOMETRY_FACTORY.createPolygon(ring);
    }

    private static List<AdminCoordinateResult> toCoordinatesList(Polygon polygon) {
        Coordinate[] coordinates = polygon.getCoordinates();
        return Arrays.stream(coordinates, 0, coordinates.length - 1)
                .map(c -> new AdminCoordinateResult(c.y, c.x))
                .toList();
    }
}
