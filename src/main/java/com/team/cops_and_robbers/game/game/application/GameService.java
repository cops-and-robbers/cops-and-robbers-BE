package com.team.cops_and_robbers.game.game.application;

import com.team.cops_and_robbers.common.dto.Coordinates;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.domain.GameAreaDomainService;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.application.dto.command.GameAreaUpdateCommand;
import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
import com.team.cops_and_robbers.game.game.application.dto.command.GameInfoCommand;
import com.team.cops_and_robbers.game.game.application.dto.command.GameSettingsUpdateCommand;
import com.team.cops_and_robbers.game.game.application.dto.result.GameAreaUpdateResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameCreateResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameInfoResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameSettingsUpdateResult;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.lobby.application.LobbyEventFactory;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {

    private static final int MAX_ATTEMPTS = 10;

    private final GameRepository gameRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final UserRepository userRepository;
    private final GameAreaDomainService gameAreaDomainService;
    private final ApplicationEventPublisher eventPublisher;
    private final LobbyEventFactory lobbyEventFactory;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);


    public GameInfoResult getGameInfo(GameInfoCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());

        if (!game.isWaiting() && !game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_ACTIVE);
        }

        gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.userId());
        return GameInfoResult.from(game);
    }

    @Transactional
    public GameCreateResult createGame(GameCreateCommand command) {

        User host = getUser(command.hostUserId());
        if (gameParticipantRepository.existsActiveGameByUserId(host.getId())) {
            throw new ApplicationException(GameParticipantException.ALREADY_PARTICIPATING);
        }

        String inviteCode = generateInviteCode();
        Game game = saveGame(command, inviteCode);
        saveGameArea(game, command);
        saveHostAsParticipant(game, host);

        return GameCreateResult.from(game);
    }

    private User getUser(Long userId) {
        User user = userRepository.getByUserId(userId);
        if (!user.hasAgreedRequiredTerms()) {
            throw  new ApplicationException(UserException.REQUIRED_TERMS_NOT_AGREED);
        }
        return user;
    }

    private Game saveGame(GameCreateCommand command, String inviteCode) {
        Game game = Game.createGame(inviteCode, command);
        return gameRepository.save(game);
    }

    private void saveGameArea(Game game, GameCreateCommand command) {
        GameArea gameArea = switch (command.areaData()) {
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
        gameAreaRepository.save(gameArea);
    }

    private void saveHostAsParticipant(Game game, User host) {
        GameParticipant hostParticipant = GameParticipant.createParticipant(game, host, true);
        gameParticipantRepository.save(hostParticipant);
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = InviteCodeGenerator.generate();
            if (!gameRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new ApplicationException(GameException.INVITE_CODE_GENERATION_FAILED);
    }

    @Transactional
    public GameAreaUpdateResult updateGameArea(GameAreaUpdateCommand command) {
        GameParticipant participant = gameParticipantRepository.getByGameIdAndUserIdWithGame(
                command.gameId(),
                command.userId()
        );
        validateGameEditable(participant);

        GameArea gameArea = gameAreaRepository.getByGameId(command.gameId());
        switch (command.areaData()) {
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
        };

        GameAreaUpdateResult result = GameAreaUpdateResult.from(gameArea);

        LobbyEvent areaEvent = lobbyEventFactory.createAreaUpdatedEvent(command.gameId(), result);
        eventPublisher.publishEvent(areaEvent);

        return result;
    }

    @Transactional
    public GameSettingsUpdateResult updateGameSettings(GameSettingsUpdateCommand command) {
        GameParticipant participant = gameParticipantRepository.getByGameIdAndUserIdWithGame(
                command.gameId(),
                command.userId()
        );
       validateGameEditable(participant);

        Game game = participant.getGame();
        game.updateSettings(
                command.roundDurationMinutes(),
                command.locationRevealIntervalMinutes(),
                command.policeWaitMinutes(),
                command.maxParticipants()
        );

        GameSettingsUpdateResult result = GameSettingsUpdateResult.from(game);

        LobbyEvent settingsEvent = lobbyEventFactory.createSettingsUpdatedEvent(command.gameId(), result);
        eventPublisher.publishEvent(settingsEvent);

        return result;
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

    private void validateGameEditable(GameParticipant participant) {
        if (!participant.isHost()) {
            throw new ApplicationException(GameParticipantException.NOT_HOST);
        }
        if (!participant.getGame().isWaiting()) {
            throw new ApplicationException(GameException.GAME_NOT_WAITING);
        }
    }
}
