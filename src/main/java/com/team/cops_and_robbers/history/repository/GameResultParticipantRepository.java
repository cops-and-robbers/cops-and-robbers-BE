package com.team.cops_and_robbers.history.repository;

import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameResultParticipantRepository extends JpaRepository<GameResultParticipant, Long> {

    List<GameResultParticipant> findByGameResultIdIn(List<Long> gameResultIds);
}
