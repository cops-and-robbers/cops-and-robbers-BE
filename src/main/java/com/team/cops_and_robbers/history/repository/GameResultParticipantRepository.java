package com.team.cops_and_robbers.history.repository;

import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameResultParticipantRepository extends JpaRepository<GameResultParticipant, Long> {

    List<GameResultParticipant> findByGameResultIdIn(List<Long> gameResultIds);

    List<GameResultParticipant> findByGameResultId(Long gameResultId);

    Optional<GameResultParticipant> findByGameResultIdAndUserIdAndLeftAtIsNull(Long gameResultId, Long userId);
}
