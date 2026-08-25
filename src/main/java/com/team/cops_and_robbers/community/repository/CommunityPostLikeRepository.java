package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.community.domain.CommunityPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {

    boolean existsByCommunityPostIdAndUserId(Long communityPostId, Long userId);

    long countByCommunityPostId(Long communityPostId);

    @Query("""
            select new com.team.cops_and_robbers.community.repository.CommunityPostCountProjection(
                l.communityPostId, count(l)
            )
            from CommunityPostLike l
            where l.communityPostId in :postIds
            group by l.communityPostId
            """)
    List<CommunityPostCountProjection> countByPostIdIn(@Param("postIds") Collection<Long> postIds);

    /** 목록에서 내가 좋아요한 게시글만 골라내기 위한 배치 조회다. */
    @Query("""
            select l.communityPostId from CommunityPostLike l
            where l.userId = :userId and l.communityPostId in :postIds
            """)
    List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityPostLike l where l.communityPostId = :postId and l.userId = :userId")
    int deleteByCommunityPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityPostLike l where l.communityPostId = :postId")
    void deleteAllByCommunityPostId(@Param("postId") Long postId);
}
