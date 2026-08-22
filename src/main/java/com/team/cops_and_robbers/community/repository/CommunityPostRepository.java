package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    List<CommunityPost> findAllByCountryCodeOrderByCreatedAtDescIdDesc(String countryCode, Pageable pageable);

    @Query("""
            select p from CommunityPost p
            where p.countryCode = :countryCode
              and (p.createdAt < :cursorCreatedAt
                or (p.createdAt = :cursorCreatedAt and p.id < :cursorId))
            order by p.createdAt desc, p.id desc
            """)
    List<CommunityPost> findPageByCursor(
            @Param("countryCode") String countryCode,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

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
