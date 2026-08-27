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
            select new com.team.cops_and_robbers.community.repository.CommunityChatSenderProfileProjection(
                u.nickname, u.profileIcon
            )
            from CommunityChatMember m
            join User u on u.id = m.userId
            where m.communityPostId = :postId and m.userId = :userId
            """)
    Optional<CommunityChatSenderProfileProjection> findSenderProfileByPostIdAndUserId(
            @Param("postId") Long postId, @Param("userId") Long userId);

    @Query("select m.communityPostId from CommunityChatMember m where m.userId = :userId")
    List<Long> findPostIdsByUserId(@Param("userId") Long userId);

    List<CommunityChatMember> findAllByUserId(Long userId);

    @Query("""
            select new com.team.cops_and_robbers.community.repository.CommunityChatMemberCountProjection(
                m.communityPostId, count(m)
            )
            from CommunityChatMember m
            where m.communityPostId in :postIds
            group by m.communityPostId
            """)
    List<CommunityChatMemberCountProjection> countByPostIdIn(@Param("postIds") Collection<Long> postIds);

    @Query("""
            select new com.team.cops_and_robbers.community.repository.CommunityChatMemberCountProjection(
                mem.communityPostId, count(msg)
            )
            from CommunityChatMember mem
            left join CommunityChatMessage msg
                 on msg.communityPostId = mem.communityPostId
                and (mem.lastReadMessageId is null or msg.id > mem.lastReadMessageId)
                and msg.senderId <> mem.userId
                and msg.messageType <> com.team.cops_and_robbers.community.domain.CommunityChatMessageType.SYSTEM
            where mem.userId = :userId
            group by mem.communityPostId
            """)
    List<CommunityChatMemberCountProjection> countUnreadByUserId(@Param("userId") Long userId);

    @Query("""
            select m.userId from CommunityChatMember m
            where m.communityPostId = :postId
              and m.userId <> :senderId
              and m.allowNotification = true
            """)
    List<Long> findPushTargetUserIds(@Param("postId") Long postId, @Param("senderId") Long senderId);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityChatMember m where m.communityPostId = :postId")
    void deleteAllByCommunityPostId(@Param("postId") Long postId);
}
