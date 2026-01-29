package com.team.cops_and_robbers.game.participant.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import org.springframework.data.jpa.repository.JpaRepository;
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
    boolean existsActiveGameByUserId(Long userId);

    @Query("""
        select gp
        from GameParticipant gp
        join fetch gp.user
        where gp.id = :id
    """)
    Optional<GameParticipant> findByIdWithUser(@Param("id") Long id);

    int countByGameId(Long gameId);

    /**
     * 팀별 인원 수
     */
    @Query("""
        select count(gp)
        from GameParticipant gp
        where gp.game.id = :gameId
          and gp.team = :team
    """)
    int countByGameIdAndTeam(Long gameId, Team team);

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

