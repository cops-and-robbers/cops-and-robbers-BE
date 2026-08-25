package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.community.domain.CommunityPostScrap;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommunityPostScrapRepository extends JpaRepository<CommunityPostScrap, Long> {

    boolean existsByCommunityPostIdAndUserId(Long communityPostId, Long userId);

    long countByCommunityPostId(Long communityPostId);

    @Query("""
            select new com.team.cops_and_robbers.community.repository.CommunityPostCountProjection(
                s.communityPostId, count(s)
            )
            from CommunityPostScrap s
            where s.communityPostId in :postIds
            group by s.communityPostId
            """)
    List<CommunityPostCountProjection> countByPostIdIn(@Param("postIds") Collection<Long> postIds);

    @Query("""
            select s.communityPostId from CommunityPostScrap s
            where s.userId = :userId and s.communityPostId in :postIds
            """)
    List<Long> findScrappedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);

    @Query("""
            select s from CommunityPostScrap s
            where s.userId = :userId and (:cursor is null or s.id < :cursor)
            order by s.id desc
            """)
    List<CommunityPostScrap> findPageByCursor(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityPostScrap s where s.communityPostId = :postId and s.userId = :userId")
    int deleteByCommunityPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityPostScrap s where s.communityPostId = :postId")
    void deleteAllByCommunityPostId(@Param("postId") Long postId);
}
