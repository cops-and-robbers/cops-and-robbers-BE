package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityChatMemberRepository extends JpaRepository<CommunityChatMember, Long> {

    boolean existsByCommunityPostIdAndUserId(Long communityPostId, Long userId);

    Optional<CommunityChatMember> findByCommunityPostIdAndUserId(Long communityPostId, Long userId);

    int countByCommunityPostId(Long communityPostId);

    @Query("""
            select u.nickname from CommunityChatMember m
            join User u on u.id = m.userId
            where m.communityPostId = :postId and m.userId = :userId
            """)
    Optional<String> findNicknameByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityChatMember m where m.communityPostId = :postId")
    void deleteAllByCommunityPostId(@Param("postId") Long postId);
}
