package com.team.cops_and_robbers.game.participant.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameParticipantRepository extends JpaRepository<GameParticipant, Long> {

    /**
     * 유저가 참여 중인 활성 게임 여부 조회
     */
    @Query("""
        select exists(
            select 1
            from GameParticipant gp
            join gp.game g
            where gp.user.id = :userId
            and g.status in ('WAITING', 'IN_PROGRESS')
        )
        """)
    boolean existsActiveGameByUserId(@Param("userId") Long userId);

    @Query("""
        select gp
        from GameParticipant gp
        join fetch gp.user
        where gp.id = :id
    """)
    Optional<GameParticipant> findByIdWithUser(@Param("id") Long id);

    int countByGameId(Long gameId);

    int countByGameIdAndTeam(Long gameId, Team team);

    @Query("""
        select count(p)
        from GameParticipant p
        where p.game.id = :gameId
        and p.team = 'ROBBER'
        and p.status = :status
        """)
    int countByGameIdAndRobberStatus(@Param("gameId") Long gameId, @Param("status") ParticipantStatus status);

    /**
     * 게임에서 준비하지 않은 참가자가 있는지 확인
     */
    @Query("""
        select exists(
            select 1
            from GameParticipant gp
            where gp.game.id = :gameId
            and gp.isReady = false
            and gp.isHost = false
        )
        """)
    boolean existsNotReadyParticipantByGameId(@Param("gameId") Long gameId);

    /**
     * 게임의 모든 참가자 상태를 업데이트
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update GameParticipant gp
        set gp.status = :status
        where gp.game.id = :gameId
        """)
    void updateStatusByGameId(@Param("gameId") Long gameId, @Param("status") ParticipantStatus status);

    Optional<GameParticipant> findByGameIdAndUserId(Long gameId, Long userId);

    Optional<GameParticipant> findFirstByGameIdOrderByCreatedAtAsc(Long gameId);

    default GameParticipant getByGameIdAndUserId(Long gameId, Long userId) {
        return findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() ->
                        new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND)
                );
    }

    default GameParticipant getNextParticipant(Long gameId) {
        return findFirstByGameIdOrderByCreatedAtAsc(gameId)
                .orElseThrow(() ->
                        new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND)
                );
    }
}

