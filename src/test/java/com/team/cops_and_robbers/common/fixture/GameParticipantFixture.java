package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.user.domain.User;

public class GameParticipantFixture {

    public static GameParticipant HOST_PARTICIPANT(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.CHASER)
                .status(ParticipantStatus.WAITING)
                .isHost(true)
                .build();
    }

    public static GameParticipant GUEST_PARTICIPANT(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.RUNNER)
                .status(ParticipantStatus.WAITING)
                .isHost(false)
                .build();
    }

    public static GameParticipant CHASER_PARTICIPANT(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.CHASER)
                .status(ParticipantStatus.WAITING)
                .isHost(false)
                .build();
    }

    public static GameParticipant RUNNER_PARTICIPANT(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.RUNNER)
                .status(ParticipantStatus.WAITING)
                .isHost(false)
                .build();
    }

    public static GameParticipant ALIVE_CHASER(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.CHASER)
                .status(ParticipantStatus.ALIVE)
                .isHost(false)
                .build();
    }

    public static GameParticipant WAITING_CHASER(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.CHASER)
                .status(ParticipantStatus.CHASER_WAITING)
                .isHost(false)
                .build();
    }

    public static GameParticipant ALIVE_RUNNER(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.RUNNER)
                .status(ParticipantStatus.ALIVE)
                .isHost(false)
                .build();
    }

    public static GameParticipant JAILED_RUNNER(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.RUNNER)
                .status(ParticipantStatus.JAILED)
                .isHost(false)
                .build();
    }

    public static GameParticipant HOST_WAITING_CHASER(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.CHASER)
                .status(ParticipantStatus.CHASER_WAITING)
                .isHost(true)
                .build();
    }

    public static GameParticipant EVENT_CHASER_WAITING(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.CHASER)
                .status(ParticipantStatus.CHASER_WAITING)
                .isHost(false)
                .build();
    }

    public static GameParticipant EVENT_CHASER_ALIVE(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.CHASER)
                .status(ParticipantStatus.ALIVE)
                .isHost(false)
                .build();
    }
}
