package com.team.cops_and_robbers.history.repository;

import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
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

    @Query("select r.endReason as endReason, count(r) as count from GameResult r group by r.endReason")
    List<EndReasonCountProjection> countGroupByEndReason();

    @Query("select avg(r.durationSeconds) from GameResult r")
    Double averageDurationSeconds();

    long countByWinnerTeam(Team winnerTeam);

    @Query("""
            select r from GameResult r
            where (:endReason is null or r.endReason = :endReason)
            """)
    Page<GameResult> findAllForAdmin(
            @Param("endReason") GameEndReason endReason,
            Pageable pageable
    );
}
