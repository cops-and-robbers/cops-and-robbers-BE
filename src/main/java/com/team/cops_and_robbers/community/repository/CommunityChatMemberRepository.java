package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommunityChatMemberRepository extends JpaRepository<CommunityChatMember, Long> {

    boolean existsByCommunityPostIdAndUserId(Long communityPostId, Long userId);

    Optional<CommunityChatMember> findByCommunityPostIdAndUserId(Long communityPostId, Long userId);

    List<CommunityChatMember> findAllByCommunityPostId(Long communityPostId);

    int countByCommunityPostId(Long communityPostId);

    int countByUserId(Long userId);

    @Query("""
            select u.nickname from CommunityChatMember m
            join User u on u.id = m.userId
            where m.communityPostId = :postId and m.userId = :userId
            """)
    Optional<String> findNicknameByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Query("select m.communityPostId from CommunityChatMember m where m.userId = :userId")
    List<Long> findPostIdsByUserId(@Param("userId") Long userId);

    @Query("""
            select new com.team.cops_and_robbers.community.repository.CommunityChatMemberCountProjection(
                m.communityPostId, count(m)
            )
            from CommunityChatMember m
            where m.communityPostId in :postIds
            group by m.communityPostId
            """)
    List<CommunityChatMemberCountProjection> countByPostIdIn(@Param("postIds") Collection<Long> postIds);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityChatMember m where m.communityPostId = :postId")
    void deleteAllByCommunityPostId(@Param("postId") Long postId);
}
