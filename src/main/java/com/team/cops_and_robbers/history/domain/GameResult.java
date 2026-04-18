package com.team.cops_and_robbers.history.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.locationtech.jts.geom.Point;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Team winnerTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameEndReason endReason;

    @Column(nullable = false)
    private Integer totalPoliceCount;

    @Column(nullable = false)
    private Integer totalRobberCount;

    @Column(nullable = false)
    private Integer arrestedRobberCount;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false, columnDefinition = "GEOMETRY(POINT, 4326)")
    private Point playgroundCenter;

    @Column(nullable = false)
    private Integer playgroundRadiusInMeters;

    public static GameResult createSnapshot(
            Game game,
            Team winningTeam,
            GameEndReason endReason,
            Integer totalPolice,
            Integer totalRobber,
            Integer capturedRobber,
            Point center,
            Integer radius
    ) {
        int durationSeconds = (int) Duration
                .between(game.getStartedAt(), LocalDateTime.now())
                .getSeconds();

        return GameResult.builder()
                .gameId(game.getId())
                .winnerTeam(winningTeam)
                .endReason(endReason)
                .totalPoliceCount(totalPolice)
                .totalRobberCount(totalRobber)
                .arrestedRobberCount(capturedRobber)
                .durationSeconds(durationSeconds)
                .playgroundCenter(center)
                .playgroundRadiusInMeters(radius)
                .build();
    }
}
