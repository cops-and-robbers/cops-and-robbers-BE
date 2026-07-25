package com.team.cops_and_robbers.game.area.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.game.game.domain.Game;
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
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "game_areas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameArea extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false, unique = true)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AreaType areaType;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    private Point playgroundCenter;

    private Integer playgroundRadiusInMeters;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    private Point jailCenter;

    private Integer jailRadiusInMeters;

    @Column(columnDefinition = "GEOMETRY(POLYGON, 4326)")
    private Polygon playgroundPolygon;

    @Column(columnDefinition = "GEOMETRY(POLYGON, 4326)")
    private Polygon jailPolygon;

    public static GameArea createCircleGameArea(
            Game game,
            Point playgroundCenter,
            Integer playgroundRadiusInMeters,
            Point jailCenter,
            Integer jailRadiusInMeters
    ) {
        return GameArea.builder()
                .game(game)
                .areaType(AreaType.CIRCLE)
                .playgroundCenter(playgroundCenter)
                .playgroundRadiusInMeters(playgroundRadiusInMeters)
                .jailCenter(jailCenter)
                .jailRadiusInMeters(jailRadiusInMeters)
                .build();
    }

    public static GameArea createPolygonGameArea(
            Game game,
            Polygon playgroundPolygon,
            Polygon jailPolygon
    ) {
        return GameArea.builder()
                .game(game)
                .areaType(AreaType.POLYGON)
                .playgroundPolygon(playgroundPolygon)
                .jailPolygon(jailPolygon)
                .build();
    }

    public void updateCircle(
            Point playgroundCenter,
            Integer playgroundRadiusInMeters,
            Point jailCenter,
            Integer jailRadiusInMeters
    ) {
        this.areaType = AreaType.CIRCLE;
        this.playgroundCenter = playgroundCenter;
        this.playgroundRadiusInMeters = playgroundRadiusInMeters;
        this.jailCenter = jailCenter;
        this.jailRadiusInMeters = jailRadiusInMeters;
        this.playgroundPolygon = null;
        this.jailPolygon = null;
    }

    public void updatePolygon(
            Polygon playgroundPolygon,
            Polygon jailPolygon
    ) {
        this.areaType = AreaType.POLYGON;
        this.playgroundPolygon = playgroundPolygon;
        this.jailPolygon = jailPolygon;
        this.playgroundCenter = null;
        this.playgroundRadiusInMeters = null;
        this.jailCenter = null;
        this.jailRadiusInMeters = null;
    }
}
