package com.team.cops_and_robbers.game.area.application;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.area.application.dto.command.GameAreaCommand;
import com.team.cops_and_robbers.game.area.application.dto.result.GameAreaResult;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.domain.GameAreaDomainService;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameAreaService {

    private final GameRepository gameRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final GameAreaDomainService gameAreaDomainService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public GameAreaResult getGameArea(GameAreaCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());

        if (!game.isWaiting() && !game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_ACTIVE);
        }

        gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.userId());
        GameArea gameArea = gameAreaRepository.getByGameId(command.gameId());
        return GameAreaResult.from(gameArea);
    }

    public boolean isInsidePlayground(Long gameId, double longitude, double latitude) {
        return gameAreaDomainService.isPointInsidePlayground(gameId, longitude, latitude);
    }

    /**
     * 검증 후 새 GameArea 엔티티를 생성합니다.
     * 구역 타입(CIRCLE/POLYGON)에 따라 내부적으로 분기합니다.
     */
    public GameArea buildGameArea(Game game, GameAreaData areaData) {
        return switch (areaData) {
            case GameAreaData.CircleAreaData c -> {
                gameAreaDomainService.validateCircleAreaContainment(
                        c.playgroundLongitude(), c.playgroundLatitude(), c.playgroundRadiusInMeters(),
                        c.jailLongitude(), c.jailLatitude(), c.jailRadiusInMeters()
                );
                Point playgroundCenter = geometryFactory.createPoint(new Coordinate(c.playgroundLongitude(), c.playgroundLatitude()));
                Point jailCenter = geometryFactory.createPoint(new Coordinate(c.jailLongitude(), c.jailLatitude()));
                yield GameArea.createCircleGameArea(game, playgroundCenter, c.playgroundRadiusInMeters(), jailCenter, c.jailRadiusInMeters());
            }
            case GameAreaData.PolygonAreaData p -> {
                Polygon playgroundPolygon = createPolygon(p.playgroundPolygon());
                Polygon jailPolygon = createPolygon(p.jailPolygon());
                gameAreaDomainService.validatePolygonAreaContainment(playgroundPolygon, jailPolygon);
                yield GameArea.createPolygonGameArea(game, playgroundPolygon, jailPolygon);
            }
        };
    }

    /**
     * 검증 후 기존 GameArea 엔티티를 수정합니다.
     * 구역 타입(CIRCLE/POLYGON)에 따라 내부적으로 분기합니다.
     */
    public void applyAreaUpdate(GameArea gameArea, GameAreaData areaData) {
        switch (areaData) {
            case GameAreaData.CircleAreaData c -> {
                gameAreaDomainService.validateCircleAreaContainment(
                        c.playgroundLongitude(), c.playgroundLatitude(), c.playgroundRadiusInMeters(),
                        c.jailLongitude(), c.jailLatitude(), c.jailRadiusInMeters()
                );
                Point playgroundCenter = geometryFactory.createPoint(new Coordinate(c.playgroundLongitude(), c.playgroundLatitude()));
                Point jailCenter = geometryFactory.createPoint(new Coordinate(c.jailLongitude(), c.jailLatitude()));
                gameArea.updateCircle(playgroundCenter, c.playgroundRadiusInMeters(), jailCenter, c.jailRadiusInMeters());
            }
            case GameAreaData.PolygonAreaData p -> {
                Polygon playgroundPolygon = createPolygon(p.playgroundPolygon());
                Polygon jailPolygon = createPolygon(p.jailPolygon());
                gameAreaDomainService.validatePolygonAreaContainment(playgroundPolygon, jailPolygon);
                gameArea.updatePolygon(playgroundPolygon, jailPolygon);
            }
        }
    }

    private Polygon createPolygon(List<Coordinates> coordinatesList) {
        Coordinate[] coordinates = coordinatesList.stream()
                .map(c -> new Coordinate(c.longitude(), c.latitude()))
                .toArray(Coordinate[]::new);

        // 폴리곤은 (첫 번째 좌표 = 마지막 좌표)인 닫힌 링이어야 함
        // 따라서 첫 번째 좌표를 마지막 좌표에 복사해줌
        // 예: [A, B, C, D] → [A, B, C, D, A]
        Coordinate[] closed = new Coordinate[coordinates.length + 1];
        System.arraycopy(coordinates, 0, closed, 0, coordinates.length);
        closed[closed.length - 1] = closed[0];

        LinearRing ring = geometryFactory.createLinearRing(closed);
        return geometryFactory.createPolygon(ring);
    }
}