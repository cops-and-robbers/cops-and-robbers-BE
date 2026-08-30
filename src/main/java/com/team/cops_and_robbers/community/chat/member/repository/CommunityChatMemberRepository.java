package com.team.cops_and_robbers.community.chat.member.repository;

import com.team.cops_and_robbers.community.chat.common.repository.CommunityChatSenderProfileProjection;
import com.team.cops_and_robbers.community.chat.member.domain.CommunityChatMember;
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
            select new com.team.cops_and_robbers.community.chat.common.repository.CommunityChatSenderProfileProjection(
                u.nickname, u.profileIcon,
                case when u.termsOfServiceAgreed = true and u.privacyPolicyAgreed = true
                    and u.locationTermsAgreed = true then true else false end
            )
            from CommunityChatMember m
            join User u on u.id = m.userId
            where m.communityPostId = :postId and m.userId = :userId
            """)
    Optional<CommunityChatSenderProfileProjection> findSenderProfileByPostIdAndUserId(
            @Param("postId") Long postId, @Param("userId") Long userId);

    @Query("select m.communityPostId from CommunityChatMember m where m.userId = :userId")
    List<Long> findPostIdsByUserId(@Param("userId") Long userId);

    /**
     * 목록 갱신은 음소거와 무관하게 방의 전원에게 가야 하므로 findPushTargetUserIds를 쓰지 않는다.
     */
    @Query("select m.userId from CommunityChatMember m where m.communityPostId = :postId")
    List<Long> findUserIdsByCommunityPostId(@Param("postId") Long postId);

    List<CommunityChatMember> findAllByUserId(Long userId);

    @Query("""
            select new com.team.cops_and_robbers.community.chat.member.repository.CommunityChatMemberCountProjection(
                m.communityPostId, count(m)
            )
            from CommunityChatMember m
            where m.communityPostId in :postIds
            group by m.communityPostId
            """)
    List<CommunityChatMemberCountProjection> countByPostIdIn(@Param("postIds") Collection<Long> postIds);

    @Query("""
            select new com.team.cops_and_robbers.community.chat.member.repository.CommunityChatMemberCountProjection(
                mem.communityPostId, count(msg)
            )
            from CommunityChatMember mem
            left join CommunityChatMessage msg
                 on msg.communityPostId = mem.communityPostId
                and (mem.lastReadMessageId is null or msg.id > mem.lastReadMessageId)
                and msg.senderId <> mem.userId
                and msg.messageType <> com.team.cops_and_robbers.community.chat.common.domain.CommunityChatMessageType.SYSTEM
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
