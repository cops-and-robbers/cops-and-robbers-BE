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

    /*
     * 아래 일곱 필드는 게임이 끝나야 정해진다.
     * 확인자료는 좌표 제공이 시작되는 게임 시작 시점에 열어 두므로, 진행 중에는 비어 있다.
     */

    @Enumerated(EnumType.STRING)
    private Team winnerTeam;

    @Enumerated(EnumType.STRING)
    private GameEndReason endReason;

    private Integer totalPoliceCount;

    private Integer totalRobberCount;

    private Integer arrestedRobberCount;

    private Integer totalArrestCount;

    private Integer durationSeconds;

    /** 위치정보 수집 시작 일시 (게임 시작 시각). */
    private LocalDateTime startedAt;

    /** 위치정보 수집 종료 일시 (게임 종료 시각). */
    private LocalDateTime endedAt;

    /** 이 게임에 적용된 위치 공개 주기. 방 설정은 게임 사이에 바뀌므로 그때 값을 복사해 둔다. */
    private Integer locationRevealIntervalMinutes;

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

    /**
     * 게임이 시작될 때 확인자료를 연다. 종료돼야 알 수 있는 값은 {@link #complete} 에서 채운다.
     */
    public static GameResult openSnapshot(Game game, GameArea gameArea) {
        GameResultBuilder builder = GameResult.builder()
                .gameId(game.getId())
                .startedAt(game.getStartedAt())
                .locationRevealIntervalMinutes(game.getLocationRevealIntervalMinutes())
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

    /**
     * 게임 종료 시점의 승패와 통계를 채운다.
     * 인원 집계는 종료 시점에 남아 있던 인원이며, 게임 중 퇴장자는 참가자 명단이 따로 보존한다.
     */
    public void complete(
            Team winningTeam,
            GameEndReason endReason,
            Integer totalPolice,
            Integer totalRobber,
            Integer jailedAtEnd,
            Integer totalArrestCount
    ) {
        this.endedAt = LocalDateTime.now();
        this.durationSeconds = (int) Duration
                .between(this.startedAt, this.endedAt)
                .getSeconds();
        this.winnerTeam = winningTeam;
        this.endReason = endReason;
        this.totalPoliceCount = totalPolice;
        this.totalRobberCount = totalRobber;
        this.arrestedRobberCount = jailedAtEnd;
        this.totalArrestCount = totalArrestCount;
    }

    public boolean isCompleted() {
        return this.endReason != null;
    }
}
