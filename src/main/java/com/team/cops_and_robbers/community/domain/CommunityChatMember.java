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

    @Column(name = "allow_notification", nullable = false)
    @Builder.Default
    private boolean allowNotification = true;

    /** 아직 한 번도 안 읽었으면 null. 메시지 id가 단조 증가하므로 이 값보다 큰 것이 안 읽은 메시지다. */
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    public static CommunityChatMember createMember(Long communityPostId, Long userId) {
        return CommunityChatMember.builder()
                .communityPostId(communityPostId)
                .userId(userId)
                .build();
    }

    public void updateNotification(boolean allowNotification) {
        this.allowNotification = allowNotification;
    }

    public void readUntil(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    public boolean hasReadUpTo(Long messageId) {
        return lastReadMessageId != null && lastReadMessageId >= messageId;
    }
}
