package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    List<CommunityPost> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query("""
            select p from CommunityPost p
            where p.createdAt < :cursorCreatedAt
               or (p.createdAt = :cursorCreatedAt and p.id < :cursorId)
            order by p.createdAt desc, p.id desc
            """)
    List<CommunityPost> findPageByCursor(
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityPost c where c.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    default CommunityPost getByPostId(Long postId) {
        return findById(postId)
                .orElseThrow(() -> new ApplicationException(CommunityPostException.POST_NOT_FOUND));
    }
}
