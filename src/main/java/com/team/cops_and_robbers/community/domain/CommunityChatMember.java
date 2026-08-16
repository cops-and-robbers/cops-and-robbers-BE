package com.team.cops_and_robbers.community.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "community_chat_members")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityChatMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_post_id", nullable = false)
    private Long communityPostId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public static CommunityChatMember createMember(Long communityPostId, Long userId) {
        return CommunityChatMember.builder()
                .communityPostId(communityPostId)
                .userId(userId)
                .build();
    }
}