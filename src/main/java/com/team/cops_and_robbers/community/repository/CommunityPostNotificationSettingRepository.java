package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.community.domain.CommunityPostNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityPostNotificationSettingRepository
        extends JpaRepository<CommunityPostNotificationSetting, Long> {

    Optional<CommunityPostNotificationSetting> findByCommunityPostIdAndUserId(Long communityPostId, Long userId);

    List<CommunityPostNotificationSetting> findAllByCommunityPostId(Long communityPostId);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityPostNotificationSetting s where s.communityPostId = :postId")
    void deleteAllByCommunityPostId(@Param("postId") Long postId);
}
