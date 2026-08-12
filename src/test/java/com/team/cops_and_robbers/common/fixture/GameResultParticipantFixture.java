package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;

public class GameResultParticipantFixture {

    public static GameResultParticipant POLICE_PARTICIPANT(GameResult gameResult, Long userId, String nickname) {
        return GameResultParticipant.builder()
                .gameResult(gameResult)
                .userId(userId)
                .nickname(nickname)
                .team(Team.POLICE)
                .status(ParticipantStatus.ALIVE)
                .build();
    }

    public static GameResultParticipant ROBBER_PARTICIPANT(GameResult gameResult, Long userId, String nickname) {
        return GameResultParticipant.builder()
                .gameResult(gameResult)
                .userId(userId)
                .nickname(nickname)
                .team(Team.ROBBER)
                .status(ParticipantStatus.JAILED)
                .build();
    }
}