package com.team.cops_and_robbers.game.area.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.game.game.domain.Game;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false, columnDefinition = "GEOMETRY(POINT, 4326)")
    private Point playgroundCenter;

    @Column(nullable = false)
    private Integer playgroundRadiusInMeters;

    @Column(nullable = false, columnDefinition = "GEOMETRY(POINT, 4326)")
    private Point jailCenter;

    @Column(nullable = false)
    private Integer jailRadiusInMeters;

    public static GameArea createGameArea(
            Game game,
            Point playgroundCenter,
            Integer playgroundRadiusInMeters,
            Point jailCenter,
            Integer jailRadiusInMeters
    ) {
        return GameArea.builder()
                .game(game)
                .playgroundCenter(playgroundCenter)
                .playgroundRadiusInMeters(playgroundRadiusInMeters)
                .jailCenter(jailCenter)
                .jailRadiusInMeters(jailRadiusInMeters)
                .build();
    }
}
