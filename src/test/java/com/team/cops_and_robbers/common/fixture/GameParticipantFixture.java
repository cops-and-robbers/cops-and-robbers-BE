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
                .team(Team.POLICE)
                .status(ParticipantStatus.WAITING)
                .isReady(true)
                .isHost(true)
                .build();
    }

    public static GameParticipant GUEST_PARTICIPANT(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.ROBBER)
                .status(ParticipantStatus.WAITING)
                .isReady(false)
                .isHost(false)
                .build();
    }

    public static GameParticipant POLICE_PARTICIPANT(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.POLICE)
                .status(ParticipantStatus.WAITING)
                .isReady(false)
                .isHost(false)
                .build();
    }

    public static GameParticipant ROBBER_PARTICIPANT(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.ROBBER)
                .status(ParticipantStatus.WAITING)
                .isReady(false)
                .isHost(false)
                .build();
    }

    public static GameParticipant ALIVE_POLICE(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.POLICE)
                .status(ParticipantStatus.ALIVE)
                .isReady(true)
                .isHost(false)
                .build();
    }

    public static GameParticipant WAITING_POLICE(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.POLICE)
                .status(ParticipantStatus.POLICE_WAITING)
                .isReady(true)
                .isHost(false)
                .build();
    }

    public static GameParticipant ALIVE_ROBBER(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.ROBBER)
                .status(ParticipantStatus.ALIVE)
                .isReady(true)
                .isHost(false)
                .build();
    }

    public static GameParticipant READY_PARTICIPANT(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.ROBBER)
                .status(ParticipantStatus.WAITING)
                .isReady(true)
                .isHost(false)
                .build();
    }

    public static GameParticipant JAILED_ROBBER(Game game, User user) {
        return GameParticipant.builder()
                .game(game)
                .user(user)
                .team(Team.ROBBER)
                .status(ParticipantStatus.JAILED)
                .isReady(true)
                .isHost(false)
                .build();
    }
}
