package com.team.cops_and_robbers.game.participant.repository;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    int countByGameId(Long gameId);
}

