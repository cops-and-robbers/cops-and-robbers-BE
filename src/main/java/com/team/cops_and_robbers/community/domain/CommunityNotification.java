package com.team.cops_and_robbers.community.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 발생 시점의 제목·내용을 그대로 복사해 둔다.
 * 원본 댓글이 지워져도 알림함에 남아야 하므로 조인하지 않는다.
 */
@Entity
@Table(name = "community_notifications")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityNotification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunityNotificationType type;

    @Column(name = "community_post_id", nullable = false)
    private Long communityPostId;

    @Column(nullable = false, length = 100)
    private String postTitle;

    @Column(nullable = false, length = 500)
    private String content;

    public static CommunityNotification createNotification(
            Long userId,
            CommunityNotificationType type,
            Long communityPostId,
            String postTitle,
            String content
    ) {
        return CommunityNotification.builder()
                .userId(userId)
                .type(type)
                .communityPostId(communityPostId)
                .postTitle(postTitle)
                .content(content)
                .build();
    }

    public boolean isRead(LocalDateTime readAt) {
        return readAt != null && !getCreatedAt().isAfter(readAt);
    }
}
