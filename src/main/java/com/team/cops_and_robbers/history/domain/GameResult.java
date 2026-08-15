package com.team.cops_and_robbers.history.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.Team;
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
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

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
    private Integer totalArrestCount;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AreaType areaType;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    private Point playgroundCenter;

    private Integer playgroundRadiusInMeters;

    @Column(columnDefinition = "GEOMETRY(POLYGON, 4326)")
    private Polygon playgroundPolygon;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    private Point jailCenter;

    private Integer jailRadiusInMeters;

    @Column(columnDefinition = "GEOMETRY(POLYGON, 4326)")
    private Polygon jailPolygon;

    public static GameResult createSnapshot(
            Game game,
            Team winningTeam,
            GameEndReason endReason,
            Integer totalPolice,
            Integer totalRobber,
            Integer jailedAtEnd,
            Integer totalArrestCount,
            GameArea gameArea
    ) {
        int durationSeconds = (int) Duration
                .between(game.getStartedAt(), LocalDateTime.now())
                .getSeconds();

        GameResultBuilder builder = GameResult.builder()
                .gameId(game.getId())
                .winnerTeam(winningTeam)
                .endReason(endReason)
                .totalPoliceCount(totalPolice)
                .totalRobberCount(totalRobber)
                .arrestedRobberCount(jailedAtEnd)
                .totalArrestCount(totalArrestCount)
                .durationSeconds(durationSeconds)
                .areaType(gameArea.getAreaType());

        if (gameArea.getAreaType() == AreaType.CIRCLE) {
            builder.playgroundCenter(gameArea.getPlaygroundCenter())
                   .playgroundRadiusInMeters(gameArea.getPlaygroundRadiusInMeters())
                   .jailCenter(gameArea.getJailCenter())
                   .jailRadiusInMeters(gameArea.getJailRadiusInMeters());
        }
        else {
            builder.playgroundPolygon(gameArea.getPlaygroundPolygon())
                   .jailPolygon(gameArea.getJailPolygon());
        }

        return builder.build();
    }
}
