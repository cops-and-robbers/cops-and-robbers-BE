package com.team.cops_and_robbers.community.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostCreateCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostUpdateCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "community_posts")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime meetingAt;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RecruitmentStatus status = RecruitmentStatus.RECRUITING;

    public static CommunityPost createPost(CommunityPostCreateCommand command) {
        return CommunityPost.builder()
                .userId(command.userId())
                .title(command.title())
                .content(command.content())
                .meetingAt(command.meetingAt())
                .latitude(command.latitude())
                .longitude(command.longitude())
                .maxParticipants(command.maxParticipants())
                .build();
    }

    public void updatePost(CommunityPostUpdateCommand command) {
        this.title = command.title();
        this.content = command.content();
        this.meetingAt = command.meetingAt();
        this.latitude = command.latitude();
        this.longitude = command.longitude();
        this.maxParticipants = command.maxParticipants();
    }

    public void updateStatus(RecruitmentStatus status) {
        this.status = status;
    }
}
