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

import java.time.Duration;
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

    private Long userId;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status;

    private LocalDateTime leftAt;

    @Column(nullable = false)
    private Integer arrestCount;

    @Column(nullable = false)
    private Integer arrestedCount;

    @Column(nullable = false)
    private boolean isMvp;

    private Double distanceMeters;

    @Column(nullable = false)
    private Integer radioSeconds;

    private LocalDateTime radioLastIssuedAt;

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
                .arrestCount(0)
                .arrestedCount(0)
                .isMvp(false)
                .radioSeconds(0)
                .build();
    }

    public void incrementArrestCount() {
        this.arrestCount++;
    }

    public void incrementArrestedCount() {
        this.arrestedCount++;
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

    public void updateDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public void markMvp() {
        this.isMvp = true;
    }

    /** 직전 발급 이후 흐른 시간을 더합니다. 토큰 수명을 넘긴 구간은 수명만큼만 셉니다. */
    public void accumulateRadioSeconds(LocalDateTime issuedAt, Duration tokenTtl) {
        if (this.radioLastIssuedAt == null) {
            this.radioLastIssuedAt = issuedAt;
            return;
        }

        long elapsedSeconds = Duration.between(this.radioLastIssuedAt, issuedAt).getSeconds();
        if (elapsedSeconds <= 0) {
            return;
        }
        this.radioSeconds += (int) Math.min(elapsedSeconds, tokenTtl.getSeconds());
        this.radioLastIssuedAt = issuedAt;
    }

    public boolean hasLeft() {
        return this.leftAt != null;
    }
}
