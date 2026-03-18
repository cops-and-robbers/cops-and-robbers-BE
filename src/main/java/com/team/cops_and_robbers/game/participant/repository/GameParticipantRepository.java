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

import java.util.List;
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
            and g.status in (
                    com.team.cops_and_robbers.game.game.domain.GameStatus.WAITING,
                    com.team.cops_and_robbers.game.game.domain.GameStatus.IN_PROGRESS
                )
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

    /**
     * 게임의 모든 참가자를 유저 정보와 함께 조회
     */
    @Query("""
        select gp
        from GameParticipant gp
        join fetch gp.user
        where gp.game.id = :gameId
    """)
    List<GameParticipant> findAllByGameIdWithUser(@Param("gameId") Long gameId);

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

    /**
     * 게임의 특정 팀 참가자 상태를 업데이트
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update GameParticipant gp
        set gp.status = :status
        where gp.game.id = :gameId
        and gp.team = :team
        """)
    void updateStatusByGameIdAndTeam(@Param("gameId") Long gameId, @Param("team") Team team, @Param("status") ParticipantStatus status);

    /**
     * 게임이 종료될 때 참가자의 상태를 초기화
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update GameParticipant gp
        set gp.status = :status, gp.isReady = false
        where gp.game.id = :gameId
        """ )
    void resetAllParticipantsForNextRound(@Param("gameId") Long gameId, @Param("status") ParticipantStatus status);

    Optional<GameParticipant> findByGameIdAndUserId(Long gameId, Long userId);

    @Query("""
        select gp
        from GameParticipant gp
        join fetch gp.user
        where gp.id = :id
        and gp.game.id = :gameId
    """)
    Optional<GameParticipant> findByIdAndGameId(@Param("id") Long id, @Param("gameId") Long gameId);

    /**
     *  특정 참가자를 게임과 함께 조회
     */
    @Query("""
        select gp
        from GameParticipant gp
        join fetch gp.game
        where gp.game.id = :gameId
        and gp.user.id = :userId
    """)
    Optional<GameParticipant> findByGameIdAndUserIdWithGame(@Param("gameId") Long gameId, @Param("userId") Long userId);

    Optional<GameParticipant> findFirstByGameIdOrderByCreatedAtAsc(Long gameId);

    default GameParticipant getByGameIdAndUserId(Long gameId, Long userId) {
        return findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() ->
                        new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND)
                );
    }

    default GameParticipant getByGameIdAndUserIdWithGame(Long gameId, Long userId) {
        return findByGameIdAndUserIdWithGame(gameId, userId)
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

    default GameParticipant getParticipantById(Long id) {
        return findById(id)
                .orElseThrow(() -> new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));
    }

    default GameParticipant getByIdAndGameId(Long id, Long gameId) {
        return findByIdAndGameId(id, gameId)
                .orElseThrow(() -> new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));
    }
}
