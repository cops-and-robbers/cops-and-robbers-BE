package com.team.cops_and_robbers.game.participant.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.user.domain.User;
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
@Table(name = "participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status;

    @Column(nullable = false)
    private boolean isReady;

    @Column(nullable = false)
    private boolean isHost;

    public static GameParticipant createParticipant(Game game, User user, boolean isHost) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.getRandomTeam())
                .status(ParticipantStatus.WAITING)
                .isReady(isHost)
                .isHost(isHost)
                .build();
    }

    public static GameParticipant createEventModeParticipant(Game game, User user) {
        boolean policeMoveStarted = LocalDateTime.now().isAfter(game.getPoliceMoveStartTime());
        ParticipantStatus status = policeMoveStarted ? ParticipantStatus.ALIVE : ParticipantStatus.POLICE_WAITING;
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.POLICE)
                .status(status)
                .isReady(true)
                .isHost(false)
                .build();
    }

    public boolean isWaiting() {
        return this.status == ParticipantStatus.WAITING;
    }

    public boolean isJailed() {
        return this.status == ParticipantStatus.JAILED;
    }

    public boolean isPoliceWaiting() {
        return this.status == ParticipantStatus.POLICE_WAITING;
    }

    public boolean isRobber() {
        return this.team == Team.ROBBER;
    }

    public boolean isPolice() {
        return this.team == Team.POLICE;
    }

    public boolean isInGame(Long gameId) {
        return this.game.getId().equals(gameId);
    }

    public void promoteToHost() {
        this.isHost = true;
    }

    public void changeTeam(Team targetTeam) {
        this.team = targetTeam;
        this.isReady = false;
    }

    public void updateReady(boolean ready) {
        this.isReady = ready;
    }

    public void updateStatus(ParticipantStatus status) {
        this.status = status;
    }
}
