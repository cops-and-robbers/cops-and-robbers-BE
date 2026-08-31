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

@Entity
@Table(name = "community_chat_messages")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_key", nullable = false, length = 36)
    private String messageKey;

    @Column(name = "community_post_id", nullable = false)
    private Long communityPostId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    
    @Column(name = "sender_nickname", nullable = false, length = 30)
    private String senderNickname;

    @Column(name = "sender_profile_icon", nullable = false)
    private int senderProfileIcon;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private CommunityChatMessageType messageType;

    public static CommunityChatMessage createMessage(
            String messageKey,
            Long communityPostId,
            Long senderId,
            String senderNickname,
            int senderProfileIcon,
            String message,
            CommunityChatMessageType messageType
    ) {
        return CommunityChatMessage.builder()
                .messageKey(messageKey)
                .communityPostId(communityPostId)
                .senderId(senderId)
                .senderNickname(senderNickname)
                .senderProfileIcon(senderProfileIcon)
                .message(message)
                .messageType(messageType)
                .build();
    }
}