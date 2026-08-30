package com.team.cops_and_robbers.community.comment.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.comment.domain.CommunityComment;
import com.team.cops_and_robbers.community.comment.exception.CommunityCommentException;
import com.team.cops_and_robbers.community.post.repository.CommunityPostCountProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    /**
     * 삭제 플래그가 선 댓글은 답글이 매달려 있어 남은 것이므로 걸러내지 않는다.
     */
    @Query("""
            select c from CommunityComment c
            where c.communityPostId = :postId and c.parentId is null
              and (:cursor is null or c.id > :cursor)
            order by c.id asc
            """)
    List<CommunityComment> findRootPageByCursor(
            @Param("postId") Long postId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("select c from CommunityComment c where c.parentId in :parentIds order by c.id asc")
    List<CommunityComment> findRepliesByParentIds(@Param("parentIds") Collection<Long> parentIds);

    int countByParentId(Long parentId);

    /** 마지막 답글을 동시에 삭제할 경우를 대비하여 락 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CommunityComment c where c.id = :commentId")
    Optional<CommunityComment> findByCommentIdForUpdate(@Param("commentId") Long commentId);

    @Query("""
            select new com.team.cops_and_robbers.community.post.repository.CommunityPostCountProjection(
                c.communityPostId, count(c)
            )
            from CommunityComment c
            where c.communityPostId in :postIds and c.deleted = false
            group by c.communityPostId
            """)
    List<CommunityPostCountProjection> countByPostIdIn(@Param("postIds") Collection<Long> postIds);

    @Query("select count(c) from CommunityComment c where c.communityPostId = :postId and c.deleted = false")
    long countByPostId(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityComment c where c.communityPostId = :postId")
    void deleteAllByCommunityPostId(@Param("postId") Long postId);

    default CommunityComment getByCommentId(Long commentId) {
        return findById(commentId)
                .orElseThrow(() -> new ApplicationException(CommunityCommentException.COMMENT_NOT_FOUND));
    }

    default CommunityComment getByCommentIdForUpdate(Long commentId) {
        return findByCommentIdForUpdate(commentId)
                .orElseThrow(() -> new ApplicationException(CommunityCommentException.COMMENT_NOT_FOUND));
    }
}
