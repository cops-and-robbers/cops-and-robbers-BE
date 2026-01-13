package com.team.cops_and_robbers.game.participant.repository;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GameParticipantRepository extends JpaRepository<GameParticipant, Long> {

    /**
     * 유저가 진행 중인 게임에 참가하고 있는지 확인
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
}

