package com.team.cops_and_robbers.history.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_result_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameResultParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_result_id", nullable = false)
    private GameResult gameResult;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status;

    /** 게임 중 퇴장한 시각. 끝까지 있었으면 null. */
    private LocalDateTime leftAt;

    public static GameResultParticipant createSnapshot(
            GameResult gameResult,
            GameParticipant participant
    ) {
        return GameResultParticipant.builder()
                .gameResult(gameResult)
                .userId(participant.getUser().getId())
                .nickname(participant.getUser().getNickname())
                .team(participant.getTeam())
                .status(participant.getStatus())
                .build();
    }

    public void markLeft(LocalDateTime leftTime) {
        this.leftAt = leftTime;
    }

    /** 종료 시점 상태로 갱신합니다. 게임 중 나간 사람은 덮지 않습니다. */
    public void updateFinalStatus(ParticipantStatus finalStatus) {
        if (hasLeft() || finalStatus == null) {
            return;
        }
        this.status = finalStatus;
    }

    public boolean hasLeft() {
        return this.leftAt != null;
    }
}
