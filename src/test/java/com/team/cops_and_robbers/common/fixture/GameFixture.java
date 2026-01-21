package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;

import java.time.LocalDateTime;

public class GameFixture {

    public static Game WAITING_GAME() {
        return Game.builder()
                .inviteCode("ABC123")
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
        return Game.builder()
                .inviteCode("ABC123")
                .status(GameStatus.IN_PROGRESS)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .startedAt(LocalDateTime.now())
                .build();
    }

    public static Game FINISHED_GAME() {
        return Game.builder()
                .inviteCode("ABC123")
                .status(GameStatus.FINISHED)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .endedAt(LocalDateTime.now())
                .build();
    }

    public static Game CANCELED_GAME() {
        return Game.builder()
                .inviteCode("ABC123")
                .status(GameStatus.CANCELED)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .build();
    }
}