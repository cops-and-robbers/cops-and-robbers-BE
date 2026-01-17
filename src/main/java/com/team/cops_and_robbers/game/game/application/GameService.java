package com.team.cops_and_robbers.game.game.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.domain.GameAreaDomainService;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
import com.team.cops_and_robbers.game.game.application.dto.result.GameCreateResult;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);


    @Transactional
    public GameCreateResult createGame(Long hostUserId, GameCreateCommand command) {

        String inviteCode = generateInviteCode();
        Game game = saveGame(command, inviteCode);
        saveGameArea(game, command);
        saveHostAsParticipant(game, hostUserId);

        return GameCreateResult.from(game);
    }

    private Game saveGame(GameCreateCommand command, String inviteCode) {
        Game game = Game.createGame(inviteCode, command);
        return gameRepository.save(game);
    }

    private void saveGameArea(Game game, GameCreateCommand command) {

        gameAreaDomainService.validateAreaContainment(
                command.playgroundLongitude(), command.playgroundLatitude(), command.playgroundRadiusInMeters(),
                command.jailLongitude(), command.jailLatitude(), command.jailRadiusInMeters()
        );

        Point playgroundCenter = geometryFactory.createPoint(
                new Coordinate(command.playgroundLongitude(), command.playgroundLatitude()));
        Point jailCenter = geometryFactory.createPoint(new Coordinate(command.jailLongitude(), command.jailLatitude()));

        GameArea gameArea = GameArea.createGameArea(
                game,
                playgroundCenter,
                command.playgroundRadiusInMeters(),
                jailCenter,
                command.jailRadiusInMeters()
        );

        gameAreaRepository.save(gameArea);
    }

    private void saveHostAsParticipant(Game game, Long hostUserId) {
        User host = userRepository.getByUserId(hostUserId);

        if (gameParticipantRepository.existsActiveGameByUserId(hostUserId)) {
            throw new ApplicationException(GameParticipantException.ALREADY_PARTICIPATING);
        }

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
}
