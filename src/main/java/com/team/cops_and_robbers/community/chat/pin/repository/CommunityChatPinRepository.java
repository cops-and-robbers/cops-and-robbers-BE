package com.team.cops_and_robbers.community.chat.pin.repository;

import com.team.cops_and_robbers.community.chat.pin.domain.CommunityChatPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityChatPinRepository extends JpaRepository<CommunityChatPin, Long> {

    Optional<CommunityChatPin> findByCommunityPostId(Long communityPostId);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityChatPin p where p.communityPostId = :postId")
    void deleteByCommunityPostId(@Param("postId") Long postId);
}
