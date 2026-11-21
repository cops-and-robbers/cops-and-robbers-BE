package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;

public class GameResultParticipantFixture {

    public static GameResultParticipant CHASER_PARTICIPANT(GameResult gameResult, Long userId, String nickname) {
        return GameResultParticipant.builder()
                .gameResult(gameResult)
                .userId(userId)
                .nickname(nickname)
                .team(Team.CHASER)
                .status(ParticipantStatus.ALIVE)
                .arrestCount(0)
                .arrestedCount(0)
                .radioSeconds(0)
                .build();
    }

    public static GameResultParticipant RUNNER_PARTICIPANT(GameResult gameResult, Long userId, String nickname) {
        return GameResultParticipant.builder()
                .gameResult(gameResult)
                .userId(userId)
                .nickname(nickname)
                .team(Team.RUNNER)
                .status(ParticipantStatus.JAILED)
                .arrestCount(0)
                .arrestedCount(0)
                .radioSeconds(0)
                .build();
    }
}
