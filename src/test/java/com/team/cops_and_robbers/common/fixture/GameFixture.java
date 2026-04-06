package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class GameFixture {

    public static Game WAITING_GAME() {
        return Game.builder()
                .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                .status(GameStatus.WAITING)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .build();
    }

    public static Game WAITING_GAME(String inviteCode) {
        return Game.builder()
                .inviteCode(inviteCode)
                .status(GameStatus.WAITING)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .build();
    }

    public static Game IN_PROGRESS_GAME() {
        LocalDateTime startedAt = LocalDateTime.now();
        return Game.builder()
                .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                .status(GameStatus.IN_PROGRESS)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .startedAt(startedAt)
                .scheduledEndTime(startedAt.plusMinutes(30))
                .build();
    }

    public static Game IN_PROGRESS_GAME(LocalDateTime startedAt) {
        return Game.builder()
                .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                .status(GameStatus.IN_PROGRESS)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .startedAt(startedAt)
                .scheduledEndTime(startedAt.plusMinutes(30))
                .build();
    }

    public static Game FINISHED_GAME() {
        return Game.builder()
                .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                .status(GameStatus.FINISHED)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .lastEndedAt(LocalDateTime.now())
                .build();
    }

    public static Game CANCELED_GAME() {
        return Game.builder()
                .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                .status(GameStatus.CANCELED)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .build();
    }
}
