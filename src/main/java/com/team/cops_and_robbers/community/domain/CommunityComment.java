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

/**
 * 댓글은 2depth로 고정
 * 답글이 남아 있는 1depth 댓글을 지우면 답글이 매달릴 곳이 없어지므로 Soft Delete 채택
 */
@Entity
@Table(name = "community_comments")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_post_id", nullable = false)
    private Long communityPostId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "user_id", nullable = false)
    private Long writerId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    public static CommunityComment createComment(Long communityPostId, Long parentId, Long writerId, String content) {
        return CommunityComment.builder()
                .communityPostId(communityPostId)
                .parentId(parentId)
                .writerId(writerId)
                .content(content)
                .build();
    }

    public boolean isReply() {
        return parentId != null;
    }

    public boolean isWrittenBy(Long userId) {
        return writerId.equals(userId);
    }

    public void markDeleted() {
        this.deleted = true;
    }
}
