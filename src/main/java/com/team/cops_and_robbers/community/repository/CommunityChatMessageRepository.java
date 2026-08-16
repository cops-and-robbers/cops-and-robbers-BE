package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityChatMessageRepository extends JpaRepository<CommunityChatMessage, Long> {

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityChatMessage m where m.communityPostId = :postId")
    void deleteAllByCommunityPostId(@Param("postId") Long postId);
}