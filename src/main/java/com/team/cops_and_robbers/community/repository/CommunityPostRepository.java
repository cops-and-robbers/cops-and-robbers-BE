package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostRepository
        extends JpaRepository<CommunityPost, Long>, CommunityPostRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CommunityPost p where p.id = :postId")
    Optional<CommunityPost> findByPostIdForUpdate(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityPost c where c.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    default CommunityPost getByPostId(Long postId) {
        return findById(postId)
                .orElseThrow(() -> new ApplicationException(CommunityPostException.POST_NOT_FOUND));
    }

    default CommunityPost getByPostIdForUpdate(Long postId) {
        return findByPostIdForUpdate(postId)
                .orElseThrow(() -> new ApplicationException(CommunityPostException.POST_NOT_FOUND));
    }
}
