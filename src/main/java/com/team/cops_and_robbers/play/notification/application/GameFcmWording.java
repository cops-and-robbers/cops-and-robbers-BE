package com.team.cops_and_robbers.play.notification.application;

import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.game.participant.domain.Team;

record GameFcmWording(
        String police,
        String policeSubject,
        String robber,
        String robberSubject,
        String arrestTitle,
        String arrestBody
) {

    static GameFcmWording of(GameMode gameMode) {
        return switch (gameMode) {
            case COPS_AND_ROBBERS -> new GameFcmWording("경찰", "경찰이", "도둑", "도둑이", "도둑 체포!", "도둑이 체포되었습니다.");
            case TAG -> new GameFcmWording("술래", "술래가", "도망자", "도망자가", "도망자 잡힘!", "도망자가 잡혔습니다.");
        };
    }

    String teamName(Team team) {
        return switch (team) {
            case POLICE -> police;
            case ROBBER -> robber;
        };
    }
}
