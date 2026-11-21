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

    public SystemEvent createChaserMoveStartEvent(Long gameId) {
        SystemEventData.ChaserMoveStartData data = SystemEventData.ChaserMoveStartData.create();
        return SystemEvent.of(gameId, SystemEventType.CHASER_MOVE_START, data);
    }

    public SystemEvent createRunnerLocationRevealEvent(Long gameId, List<SystemEventData.RunnerLocation> locations) {
        SystemEventData.RunnerLocationRevealData data = SystemEventData.RunnerLocationRevealData.of(locations);
        return SystemEvent.of(gameId, SystemEventType.RUNNER_LOCATION_REVEAL, data);
    }

    public SystemEvent createArrestEvent(Long gameId, GameParticipant police, GameParticipant robber, int remainingThieves) {
        SystemEventData.ArrestData data = SystemEventData.ArrestData.of(police, robber, remainingThieves);
        return SystemEvent.of(gameId, SystemEventType.ARREST, data);
    }

    public SystemEvent createForceArrestEvent(Long gameId, GameParticipant robber, int remainingThieves) {
        SystemEventData.ArrestData data = SystemEventData.ArrestData.ofForced(robber, remainingThieves);
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

    public SystemEvent createPlayerLeftEvent(Long gameId, GameParticipant participant) {
        SystemEventData.PlayerLeftData data = SystemEventData.PlayerLeftData.of(participant);
        return SystemEvent.of(gameId, SystemEventType.PLAYER_LEFT, data);
    }
}
