package com.team.cops_and_robbers.history.repository;

import com.team.cops_and_robbers.history.domain.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    Optional<GameResult> findByGameId(Long gameId);

    List<GameResult> findByGameIdIn(List<Long> gameIds);
}
