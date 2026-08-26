package com.team.cops_and_robbers.report.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "community_post_reports", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_post_reporter_reported",
                columnNames = {"reporter_user_id", "reported_user_id", "post_id"}
        )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CommunityPostReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false, length = 100)
    private String postTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String postContent;

    @Column(nullable = false)
    private Long reporterUserId;

    @Column(nullable = false)
    private Long reportedUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @Column(columnDefinition = "TEXT")
    private String etcReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String adminMemo;

    public void updateStatus(ReportStatus status, String adminMemo) {
        this.status = status;
        this.adminMemo = adminMemo;
    }

    public static CommunityPostReport create(Long postId, String postTitle, String postContent, Long reporterUserId, Long reportedUserId, ReportType reportType, String etcReason) {
        return CommunityPostReport.builder()
                .postId(postId)
                .postTitle(postTitle)
                .postContent(postContent)
                .reporterUserId(reporterUserId)
                .reportedUserId(reportedUserId)
                .reportType(reportType)
                .etcReason(etcReason)
                .build();
    }
}
