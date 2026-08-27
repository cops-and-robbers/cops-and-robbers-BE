package com.team.cops_and_robbers.community.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토글을 건드린 게시글만 행이 생긴다.
 * 행이 없으면 그 글에서의 역할에 따라 디폴트가 정해진다.
 */
@Entity
@Table(name = "community_post_notification_settings", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_post_noti_setting_user_post",
                columnNames = {"user_id", "community_post_id"}
        )
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostNotificationSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "community_post_id", nullable = false)
    private Long communityPostId;

    @Column(nullable = false)
    private boolean notifyComments;

    @Column(nullable = false)
    private boolean notifyReplies;

    public static CommunityPostNotificationSetting createSetting(
            Long userId,
            Long communityPostId,
            boolean notifyComments,
            boolean notifyReplies
    ) {
        return CommunityPostNotificationSetting.builder()
                .userId(userId)
                .communityPostId(communityPostId)
                .notifyComments(notifyComments)
                .notifyReplies(notifyReplies)
                .build();
    }

    public void updateSetting(boolean notifyComments, boolean notifyReplies) {
        this.notifyComments = notifyComments;
        this.notifyReplies = notifyReplies;
    }

    public boolean allows(CommunityNotificationType type) {
        return type == CommunityNotificationType.REPLY ? notifyReplies : notifyComments;
    }
}
