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
