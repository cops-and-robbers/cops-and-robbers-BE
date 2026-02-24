package com.team.cops_and_robbers.history.repository;

import com.team.cops_and_robbers.history.domain.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

}
