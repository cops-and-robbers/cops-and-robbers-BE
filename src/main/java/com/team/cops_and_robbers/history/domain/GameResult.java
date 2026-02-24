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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    // TODO: 어떤 정보를 저장하고 통계를 낼지 추후 구체화
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private GameHighlights highlights;

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
            GameHighlights highlights,
            Point center,
            Integer radius
    ) {
        LocalDateTime now = LocalDateTime.now();        // 임시~~
        int durationSeconds = (int) Duration
                .between(game.getStartedAt(), now)
                .getSeconds();

        return GameResult.builder()
                .gameId(game.getId())
                .winnerTeam(winningTeam)
                .endReason(endReason)
                .totalPoliceCount(totalPolice)
                .totalRobberCount(totalRobber)
                .arrestedRobberCount(capturedRobber)
                .durationSeconds(durationSeconds)
                .highlights(highlights)
                .playgroundCenter(center)
                .playgroundRadiusInMeters(radius)
                .build();
    }
}
