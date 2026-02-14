package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SystemEventFactory {

    public SystemEvent createPoliceMoveStartEvent(Long gameId) {
        SystemEventData.PoliceMoveStartData data = SystemEventData.PoliceMoveStartData.create();
        return SystemEvent.of(gameId, SystemEventType.POLICE_MOVE_START, data);
    }

    public SystemEvent createRobberLocationRevealEvent(Long gameId, List<SystemEventData.RobberLocation> locations) {
        SystemEventData.RobberLocationRevealData data = SystemEventData.RobberLocationRevealData.of(locations);
        return SystemEvent.of(gameId, SystemEventType.ROBBER_LOCATION_REVEAL, data);
    }

    public SystemEvent createArrestEvent(Long gameId, GameParticipant police, GameParticipant robber, int remainingThieves) {
        SystemEventData.ArrestData data = SystemEventData.ArrestData.of(police, robber, remainingThieves);
        return SystemEvent.of(gameId, SystemEventType.ARREST, data);
    }

    public SystemEvent createEscapeEvent(Long gameId, GameParticipant escapedThief, int remainingThieves) {
        SystemEventData.EscapeData data = SystemEventData.EscapeData.of(escapedThief, remainingThieves);
        return SystemEvent.of(gameId, SystemEventType.ESCAPE, data);
    }

    public SystemEvent createGameEndEvent(Long gameId, Team winnerTeam, GameEndReason reason, Long gameResultId) {
        SystemEventData.GameEndData data = SystemEventData.GameEndData.of(gameResultId, winnerTeam, reason);
        return SystemEvent.of(gameId, SystemEventType.GAME_OVER, data);
    }
}
