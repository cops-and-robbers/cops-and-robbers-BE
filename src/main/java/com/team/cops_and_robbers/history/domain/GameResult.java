package com.team.cops_and_robbers.history.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Team winningTeam;

    @Column(nullable = false)
    private Integer totalPoliceCount;

    @Column(nullable = false)
    private Integer totalRobberCount;

    @Column(nullable = false)
    private Integer capturedRobberCount;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false, columnDefinition = "GEOMETRY(POINT, 4326)")
    private Point playgroundCenter;

    @Column(nullable = false)
    private Integer playgroundRadiusInMeters;
}
