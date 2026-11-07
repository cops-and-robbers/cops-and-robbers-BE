package com.team.cops_and_robbers.game.area.domain;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.exception.GameAreaException;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameAreaDomainService {

    private static final int NO_JAIL_RADIUS = 0;

    private final GameAreaRepository gameAreaRepository;

    /**
     * 게임 구역의 공간적 포함 관계를 검증합니다.
     *
     * 1. 감옥 반지름 < 플레이그라운드 반지름
     * 2. 감옥이 플레이그라운드 영역 내에 완전히 포함
     * 구현: postgis의 st_distance를 활용
     */
    public void validateCircleAreaContainment(
            double playgroundLongitude, double playgroundLatitude, int playgroundRadius,
            double jailLongitude, double jailLatitude, int jailRadius
    ) {
        if (jailRadius >= playgroundRadius) {
            throw new ApplicationException(GameAreaException.INVALID_JAIL_RADIUS);
        }

        boolean isContained = gameAreaRepository.isCircleContained(
                playgroundLongitude, playgroundLatitude, playgroundRadius,
                jailLongitude, jailLatitude, jailRadius
        );

        if (!isContained) {
            throw new ApplicationException(GameAreaException.JAIL_OUTSIDE_PLAYGROUND);
        }
    }

    /**
     * 게임 구역의 공간적 포함 관계를 검증합니다.
     *
     * 1. 감옥 폴리곤 < 플레이그라운드 폴리곤
     * 2. 감옥이 플레이그라운드 영역 내에 완전히 포함
     * 구현: postgis의 st_contains를 활용
     */
    public void validatePolygonAreaContainment(
            Polygon playgroundPolygon,
            Polygon jailPolygon
    ) {
        boolean isContained = gameAreaRepository.isPolygonContained(playgroundPolygon.toText(), jailPolygon.toText());

        if (!isContained) {
            throw new ApplicationException(GameAreaException.JAIL_OUTSIDE_PLAYGROUND);
        }
    }

    /**
     * 원형 구역의 축소 최종 반경을 검증합니다.
     *
     * 1. null 이면 축소를 쓰지 않으므로 검증하지 않음
     * 2. 감옥 반지름 < 최종 반경 < 플레이그라운드 반지름
     * 감옥이 없는 구역(술래잡기)은 감옥 반지름을 0으로 본다
     */
    public void validateCircleShrinkFinalRadius(
            Integer playgroundRadiusInMeters, Integer jailRadiusInMeters, Integer shrinkFinalRadiusInMeters
    ) {
        if (shrinkFinalRadiusInMeters == null) {
            return;
        }

        int jailRadius = jailRadiusInMeters == null ? NO_JAIL_RADIUS : jailRadiusInMeters;
        if (shrinkFinalRadiusInMeters <= jailRadius || shrinkFinalRadiusInMeters >= playgroundRadiusInMeters) {
            throw new ApplicationException(GameAreaException.INVALID_SHRINK_FINAL_RADIUS);
        }
    }

    /**
     * 폴리곤 구역은 구역 축소를 지원하지 않습니다.
     */
    public void validatePolygonShrinkFinalRadius(Integer shrinkFinalRadiusInMeters) {
        if (shrinkFinalRadiusInMeters != null) {
            throw new ApplicationException(GameAreaException.SHRINK_REQUIRES_CIRCLE_AREA);
        }
    }

    /**
     * 저장된 구역을 기준으로 축소 최종 반경을 검증합니다.
     * 구역 타입(CIRCLE/POLYGON)에 따라 내부적으로 분기합니다.
     */
    public void validateShrinkFinalRadius(GameArea gameArea, Integer shrinkFinalRadiusInMeters) {
        switch (gameArea.getAreaType()) {
            case CIRCLE -> validateCircleShrinkFinalRadius(
                    gameArea.getPlaygroundRadiusInMeters(), gameArea.getJailRadiusInMeters(),
                    shrinkFinalRadiusInMeters
            );
            case POLYGON -> validatePolygonShrinkFinalRadius(shrinkFinalRadiusInMeters);
        }
    }

    /**
     * 주어진 좌표가 플레이그라운드 영역 안에 있는지 확인합니다.
     * 구역 타입(CIRCLE/POLYGON)에 따라 내부적으로 분기합니다.
     */
    public boolean isPointInsidePlayground(Long gameId, double longitude, double latitude) {
        GameArea gameArea = gameAreaRepository.getByGameId(gameId);
        return switch (gameArea.getAreaType()) {
            case CIRCLE -> gameAreaRepository.isPointInsideCircle(gameId, longitude, latitude);
            case POLYGON -> gameAreaRepository.isPointInsidePolygon(gameId, longitude, latitude);
        };
    }
}
