package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

public class GameFixture {

    public static Game WAITING_GAME() {
        Game game = Game.builder()
                .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                .status(GameStatus.WAITING)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .build();
        ReflectionTestUtils.setField(game, "createdAt", LocalDateTime.now());
        return game;
    }

    public static Game WAITING_GAME(String inviteCode) {
        Game game = Game.builder()
                .inviteCode(inviteCode)
                .status(GameStatus.WAITING)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .build();
        ReflectionTestUtils.setField(game, "createdAt", LocalDateTime.now());
        return game;
    }

    public static Game IN_PROGRESS_GAME() {
        Game game = Game.builder()
                .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                .status(GameStatus.IN_PROGRESS)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .startedAt(LocalDateTime.now())
                .roundNumber(1)
                .build();
        ReflectionTestUtils.setField(game, "createdAt", LocalDateTime.now());
        return game;
    }

    public static Game IN_PROGRESS_GAME(LocalDateTime startedAt) {
        Game game = Game.builder()
                .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                .status(GameStatus.IN_PROGRESS)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .startedAt(startedAt)
                .roundNumber(1)
                .build();
        ReflectionTestUtils.setField(game, "createdAt", LocalDateTime.now());
        return game;
    }

    public static Game FINISHED_GAME() {
        Game game = Game.builder()
                .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                .status(GameStatus.FINISHED)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .lastEndedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(game, "createdAt", LocalDateTime.now());
        return game;
    }

    public static Game CANCELED_GAME() {
        Game game = Game.builder()
                .inviteCode(UUID.randomUUID().toString().substring(0, 6))
                .status(GameStatus.CANCELED)
                .roundDurationMinutes(30)
                .locationRevealIntervalMinutes(5)
                .policeWaitMinutes(3)
                .maxParticipants(10)
                .build();
        ReflectionTestUtils.setField(game, "createdAt", LocalDateTime.now());
        return game;
    }
}
