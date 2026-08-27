package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.community.domain.CommunityNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommunityNotificationRepository extends JpaRepository<CommunityNotification, Long> {

    @Query("""
            select n from CommunityNotification n
            where n.userId = :userId and n.createdAt >= :since
              and (:cursor is null or n.id < :cursor)
            order by n.id desc
            """)
    List<CommunityNotification> findPageByCursor(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            select count(n) from CommunityNotification n
            where n.userId = :userId and n.createdAt >= :since
              and (cast(:readAt as timestamp) is null or n.createdAt > :readAt)
            """)
    long countUnread(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("readAt") LocalDateTime readAt
    );

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityNotification n where n.communityPostId = :postId")
    void deleteAllByCommunityPostId(@Param("postId") Long postId);
}
