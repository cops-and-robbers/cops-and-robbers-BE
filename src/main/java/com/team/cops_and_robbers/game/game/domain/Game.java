package com.team.cops_and_robbers.game.game.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Game extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Column(nullable = false)
    private Integer roundDurationMinutes;

    @Column(nullable = false)
    private Integer locationRevealIntervalMinutes;

    @Column(nullable = false)
    private Integer policeWaitMinutes;

    @Column(nullable = false)
    private Integer maxParticipants;

    private LocalDateTime startedAt;

    private LocalDateTime lastEndedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean isEventGame = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalArrestCount = 0;

    @Column(nullable = false)
    private int roundNumber = 0;

    public boolean isWaiting() {
        return this.status == GameStatus.WAITING;
    }

    public boolean isInProgress() {
        return this.status == GameStatus.IN_PROGRESS;
    }

    public void startGame(LocalDateTime startTime) {
        this.startedAt = startTime;
        this.status = GameStatus.IN_PROGRESS;
        this.roundNumber++;
    }

    public void resetForNextRound() {
        this.status = GameStatus.WAITING;
        this.lastEndedAt = LocalDateTime.now();
        this.totalArrestCount = 0;
    }

    public static Game createGame(String inviteCode, GameCreateCommand command) {
        return Game.builder()
                .inviteCode(inviteCode)
                .status(GameStatus.WAITING)
                .roundDurationMinutes(command.roundDurationMinutes())
                .locationRevealIntervalMinutes(command.locationRevealIntervalMinutes())
                .policeWaitMinutes(command.policeWaitMinutes())
                .maxParticipants(command.maxParticipants())
                .build();
    }

    public void incrementArrestCount() {
        this.totalArrestCount++;
    }

    public void updateSettings(
            Integer roundDurationMinutes,
            Integer locationRevealIntervalMinutes,
            Integer policeWaitMinutes,
            Integer maxParticipants
    ) {
        this.roundDurationMinutes = roundDurationMinutes;
        this.locationRevealIntervalMinutes = locationRevealIntervalMinutes;
        this.policeWaitMinutes = policeWaitMinutes;
        this.maxParticipants = maxParticipants;
    }

    public boolean isSameRound(int eventRound) {
        return this.roundNumber == eventRound;
    }

    public boolean isGameOverTimeReached(LocalDateTime now) {
        return !now.isBefore(getGameOverTime());
    }

    public boolean isBeforeGameOver(LocalDateTime time) {
        return time.isBefore(getGameOverTime());
    }

    public LocalDateTime getPoliceMoveStartTime() {
        return this.startedAt.plusMinutes(this.policeWaitMinutes);
    }

    public LocalDateTime getGameOverTime() {
        return this.startedAt.plusMinutes(this.roundDurationMinutes);
    }

    public boolean isLocationRevealDisabled() {
        return this.locationRevealIntervalMinutes == 0;
    }

    public LocalDateTime getFirstRobberRevealTime() {
        return getPoliceMoveStartTime().plusMinutes(this.locationRevealIntervalMinutes);
    }
}
