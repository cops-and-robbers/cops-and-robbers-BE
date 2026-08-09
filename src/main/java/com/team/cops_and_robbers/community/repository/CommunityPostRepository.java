package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    Page<CommunityPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityPost c where c.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    default CommunityPost getByPostId(Long postId) {
        return findById(postId)
                .orElseThrow(() -> new ApplicationException(CommunityPostException.POST_NOT_FOUND));
    }
}
