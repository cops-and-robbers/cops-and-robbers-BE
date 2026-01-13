package com.team.cops_and_robbers.game.game.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {

    boolean existsByInviteCode(String inviteCode);

    default Game getByGameId(Long gameId) {
        return findById(gameId)
                .orElseThrow(() -> new ApplicationException(GameException.GAME_NOT_FOUND));
    }

}
