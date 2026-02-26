package com.team.cops_and_robbers.game.area.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.application.dto.command.GameAreaCommand;
import com.team.cops_and_robbers.game.area.application.dto.result.GameAreaResult;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameAreaService {

    private final GameRepository gameRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;

    public GameAreaResult getGameArea(GameAreaCommand command) {
        Game game = gameRepository.getByGameId(command.gameId());

        if (!game.isWaiting() && !game.isInProgress()) {
            throw new ApplicationException(GameException.GAME_NOT_ACTIVE);
        }

        gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.userId());
        GameArea gameArea = gameAreaRepository.getByGameId(command.gameId());
        return GameAreaResult.from(gameArea);
    }
}
