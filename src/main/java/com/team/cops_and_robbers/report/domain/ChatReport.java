package com.team.cops_and_robbers.report.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reports", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_reporter_reported_game",
                columnNames = {"reporter_user_id", "reported_user_id", "game_id"}
        )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private Long reporterUserId;

    @Column(nullable = false)
    private Long reportedUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    public static ChatReport create(Long gameId, Long reporterUserId, Long reportedUserId, String messageContent) {
        return ChatReport.builder()
                .gameId(gameId)
                .reporterUserId(reporterUserId)
                .reportedUserId(reportedUserId)
                .messageContent(messageContent)
                .build();
    }
}
