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

    public boolean isWaiting() {
        return this.status == GameStatus.WAITING;
    }

    public boolean isInProgress() {
        return this.status == GameStatus.IN_PROGRESS;
    }

    public void startGame(LocalDateTime startTime) {
        this.startedAt = startTime;
        this.status = GameStatus.IN_PROGRESS;
    }

    public void resetForNextRound() {
        this.status = GameStatus.WAITING;
        this.lastEndedAt = LocalDateTime.now();
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
}
