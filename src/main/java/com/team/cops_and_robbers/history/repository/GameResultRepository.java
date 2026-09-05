package com.team.cops_and_robbers.history.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.exception.GameResultException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    Optional<GameResult> findByGameId(Long gameId);

    List<GameResult> findByGameIdIn(List<Long> gameIds);

    /**
     * 아직 종료되지 않은 확인자료 조회. 게임 시작 시점에 열어 둔 행이며 게임당 최대 하나다.
     */
    Optional<GameResult> findByGameIdAndEndReasonIsNull(Long gameId);

    @Query("""
            select r.endReason as endReason, count(r) as count from GameResult r
            where r.endReason is not null
            group by r.endReason
            """)
    List<EndReasonCountProjection> countGroupByEndReason();

    @Query("select avg(r.durationSeconds) from GameResult r")
    Double averageDurationSeconds();

    long countByWinnerTeam(Team winnerTeam);

    @Query("""
            select r from GameResult r
            where r.endReason is not null
            and (:endReason is null or r.endReason = :endReason)
            """)
    Page<GameResult> findAllForAdmin(
            @Param("endReason") GameEndReason endReason,
            Pageable pageable
    );

    default GameResult getByGameResultId(Long gameResultId) {
        return findById(gameResultId)
                .orElseThrow(() -> new ApplicationException(GameResultException.GAME_RESULT_NOT_FOUND));
    }
}
