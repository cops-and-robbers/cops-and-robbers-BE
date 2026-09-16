package com.team.cops_and_robbers.history.repository;

import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface GameResultParticipantRepository extends JpaRepository<GameResultParticipant, Long> {

    List<GameResultParticipant> findByGameResultIdIn(List<Long> gameResultIds);

    List<GameResultParticipant> findByGameResultId(Long gameResultId);

    /**
     * 체포수·잡힌 횟수 증가와 퇴장 시각 기록이 모두 이 조회를 거쳐 값을 바꾸므로 행을 잠급니다.
     * 잠그지 않으면 같은 경찰이 연달아 체포하거나, 이벤트 게임에서 여러 경찰이
     * 같은 도둑을 동시에 잡을 때 증가분이 서로 덮입니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GameResultParticipant> findByGameResultIdAndUserIdAndLeftAtIsNull(Long gameResultId, Long userId);

    Optional<GameResultParticipant> findFirstByGameResultIdAndUserIdOrderByIdDesc(Long gameResultId, Long userId);
}
