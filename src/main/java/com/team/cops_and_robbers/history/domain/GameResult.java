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

    /** 같은 방에서 몇 번째 게임인지. */
    private Integer roundNumber;

    @Enumerated(EnumType.STRING)
    private Team winnerTeam;

    @Enumerated(EnumType.STRING)
    private GameEndReason endReason;

    private Integer totalPoliceCount;

    private Integer totalRobberCount;

    private Integer arrestedRobberCount;

    private Integer totalArrestCount;

    private Integer durationSeconds;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    /** 이 게임에 적용된 위치 공개 주기. 방 설정은 게임 사이에 바뀌므로 그때 값을 복사해 둔다. */
    private Integer locationRevealIntervalMinutes;

    @Column(name = "is_event_game")
    private Boolean eventGame;

    private Long hostUserId;

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
     * 게임 시작 시점의 값으로 GameResult 를 엽니다. 나머지는 complete 에서 채웁니다.
     */
    public static GameResult openSnapshot(Game game, GameArea gameArea, Long hostUserId) {
        GameResultBuilder builder = GameResult.builder()
                .gameId(game.getId())
                .roundNumber(game.getRoundNumber())
                .startedAt(game.getStartedAt())
                .locationRevealIntervalMinutes(game.getLocationRevealIntervalMinutes())
                .eventGame(game.isEventGame())
                .hostUserId(hostUserId)
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
     * 게임 종료 시점의 승패와 통계를 채웁니다.
     */
    public void complete(
            LocalDateTime endTime,
            Team winningTeam,
            GameEndReason endReason,
            Integer totalPolice,
            Integer totalRobber,
            Integer jailedAtEnd,
            Integer totalArrestCount
    ) {
        this.endedAt = endTime;
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

    /**
     * 진행 중에 이벤트 게임으로 전환되면 시작 시점에 복사해 둔 값을 따라 바꿉니다.
     */
    public void updateEventGame(boolean eventGame) {
        this.eventGame = eventGame;
    }

    public boolean isCompleted() {
        return this.endReason != null;
    }
}
