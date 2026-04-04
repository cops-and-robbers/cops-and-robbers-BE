package com.team.cops_and_robbers.game.game.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    @Modifying(clearAutomatically = true)
    @Query("delete from Game g where g.id = :gameId")
    void deleteByGameId(@Param("gameId") Long gameId);

    boolean existsByInviteCode(String inviteCode);

    List<Game> findByStatus(GameStatus status);

    Optional<Game> findByInviteCode(String inviteCode);

    default Game getByGameId(Long gameId) {
        return findById(gameId)
                .orElseThrow(() -> new ApplicationException(GameException.GAME_NOT_FOUND));
    }

    default Game getByInviteCode(String inviteCode) {
        return findByInviteCode(inviteCode)
                .orElseThrow(() -> new ApplicationException(GameParticipantException.INVALID_INVITE_CODE));
    }

}
