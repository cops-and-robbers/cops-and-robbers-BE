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
import java.util.Set;

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

    /**
     * 유저가 참여 중인 활성 게임 참가자 조회
     */
    @Query("""
        select gp
        from GameParticipant gp
        join fetch gp.game g
        where gp.user.id = :userId
        and g.status in (
                com.team.cops_and_robbers.game.game.domain.GameStatus.WAITING,
                com.team.cops_and_robbers.game.game.domain.GameStatus.IN_PROGRESS
            )
        """)
    Optional<GameParticipant> findActiveParticipantByUserId(@Param("userId") Long userId);

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
     * 현재 잡히지 않는 상태의 도둑 id 들을 조회
     */
    @Query("""
        select gp.id
        from GameParticipant gp
        where gp.game.id = :gameId
        and gp.team = 'ROBBER'
        and gp.status = 'ALIVE'
        """)
    Set<Long> findAliveRobberIdsByGameId(@Param("gameId") Long gameId);

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

    @Modifying(clearAutomatically = true)
    @Query("delete from GameParticipant gp where gp.game.id = :gameId")
    void deleteAllByGameId(@Param("gameId") Long gameId);

    Optional<GameParticipant> findFirstByGameIdOrderByCreatedAtAsc(Long gameId);

    @Query("""
        select gp
        from GameParticipant gp
        join fetch gp.game
        join fetch gp.user
        where gp.user.id in :userIds
        """)
    List<GameParticipant> findByUserIdInWithGame(@Param("userIds") List<Long> userIds);

    @Query("""
        select gp
        from GameParticipant gp
        join fetch gp.user
        join fetch gp.game
        where gp.game.id in :gameIds
        """)
    List<GameParticipant> findByGameIdInWithUser(@Param("gameIds") List<Long> gameIds);

    @Query("""
        select new com.team.cops_and_robbers.game.participant.repository.GameParticipantCountProjection(
            gp.game.id, count(gp)
        )
        from GameParticipant gp
        where gp.game.id in :gameIds
        group by gp.game.id
        """)
    List<GameParticipantCountProjection> countByGameIdIn(@Param("gameIds") List<Long> gameIds);

    @Query("""
        select new com.team.cops_and_robbers.game.participant.repository.GameParticipantCacheProjection(
            gp.id, gp.user.nickname, gp.team, ud.fcmToken
        )
        from GameParticipant gp
        join gp.user u
        left join UserDevice ud on ud.user.id = u.id and ud.fcmToken is not null and u.allowGamePush = true
        where gp.game.id = :gameId
        """)
    List<GameParticipantCacheProjection> findCacheProjectionsByGameId(@Param("gameId") Long gameId);

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
