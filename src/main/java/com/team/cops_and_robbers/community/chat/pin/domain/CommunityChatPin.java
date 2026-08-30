package com.team.cops_and_robbers.community.chat.pin.domain;

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
@Table(name = "community_chat_pins")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityChatPin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_post_id", nullable = false, unique = true)
    private Long communityPostId;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    @Column(nullable = false, length = 500)
    private String content;

    public static CommunityChatPin createPin(Long communityPostId, Long writerId, String content) {
        return CommunityChatPin.builder()
                .communityPostId(communityPostId)
                .writerId(writerId)
                .content(content)
                .build();
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
