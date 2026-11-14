package com.team.cops_and_robbers.history.repository;

import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameResultParticipantRepository extends JpaRepository<GameResultParticipant, Long> {

    List<GameResultParticipant> findByGameResultIdIn(List<Long> gameResultIds);

    List<GameResultParticipant> findByGameResultId(Long gameResultId);

    List<GameResultParticipant> findByGameResultIdAndIsMvpTrue(Long gameResultId);

    /**
     * 체포수·잡힌 횟수 증가와 퇴장 시각 기록이 모두 이 조회를 거쳐 값을 바꾸므로 행을 잠급니다.
     * 잠그지 않으면 같은 경찰이 연달아 체포하거나, 이벤트 게임에서 여러 경찰이
     * 같은 도둑을 동시에 잡을 때 증가분이 서로 덮입니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GameResultParticipant> findByGameResultIdAndUserIdAndLeftAtIsNull(Long gameResultId, Long userId);

    Optional<GameResultParticipant> findFirstByGameResultIdAndUserIdOrderByIdDesc(Long gameResultId, Long userId);

    /**
     * 유저가 참가한 끝난 게임 기록을 최신순으로 조회합니다.
     * 이벤트 게임에서 나갔다 다시 들어오면 같은 판에 행이 여러 개라, 개인 기록 조회와 같이 가장 최근 행만 씁니다.
     */
    @Query("""
            select p from GameResultParticipant p
            join fetch p.gameResult r
            where p.userId = :userId
            and r.endReason is not null
            and p.id = (
                select max(p2.id) from GameResultParticipant p2
                where p2.gameResult = r and p2.userId = :userId
            )
            order by r.createdAt desc, r.id desc
            """)
    List<GameResultParticipant> findLatestCompletedByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 유저들이 참가한 끝난 게임 기록을 판과 함께 읽습니다. 어드민에서 유저가 주로 플레이하는 국가를 셀 때 씁니다.
     * 이벤트 게임에서 나갔다 다시 들어오면 같은 판에 행이 여러 개라, 판 단위로 세는 쪽에서 같은 판을 한 번만 셉니다.
     */
    @Query("""
            select p from GameResultParticipant p
            join fetch p.gameResult r
            where p.userId in :userIds
            and r.endReason is not null
            """)
    List<GameResultParticipant> findCompletedWithResultByUserIdIn(@Param("userIds") List<Long> userIds);

    @Query("""
            select p.userId as userId, sum(p.radioSeconds) as radioSeconds
            from GameResultParticipant p
            where p.userId in :userIds
            group by p.userId
            """)
    List<RadioUsageProjection> sumRadioSecondsGroupByUserIdIn(@Param("userIds") List<Long> userIds);

    /**
     * 탈퇴자의 참가 기록에서 탈퇴자와 이어지는 값만 비웁니다.
     * 행과 수치는 같은 판 다른 참가자의 결과 화면에 쓰이므로 남깁니다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update GameResultParticipant p
        set p.userId = null, p.nickname = :nickname
        where p.userId = :userId
        """)
    void anonymizeByUserId(@Param("userId") Long userId, @Param("nickname") String nickname);
}
