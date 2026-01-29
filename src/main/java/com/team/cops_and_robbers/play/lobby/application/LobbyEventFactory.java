package com.team.cops_and_robbers.play.lobby.application;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEventData;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEventType;
import org.springframework.stereotype.Component;

@Component
public class LobbyEventFactory {

    public LobbyEvent createEnterEvent(Long gameId, GameParticipant participant, int currentCount, int maxCount) {
        LobbyEventData.LobbyEnterData data = LobbyEventData.LobbyEnterData.of(participant, currentCount, maxCount);
        return LobbyEvent.of(gameId, LobbyEventType.ENTER, data);
    }

    public LobbyEvent createExitEvent(Long gameId, Long exitedParticipantId, int currentCount, int maxCount) {
        LobbyEventData.LobbyExitData data = LobbyEventData.LobbyExitData.of(exitedParticipantId, currentCount, maxCount);
        return LobbyEvent.of(gameId, LobbyEventType.EXIT, data);
    }

    public LobbyEvent createTeamUpdateEvent(Long gameId, GameParticipant participant, int policeCount, int robberCount) {
        LobbyEventData.LobbyTeamUpdateData data = LobbyEventData.LobbyTeamUpdateData.of(participant, policeCount, robberCount);
        return LobbyEvent.of(gameId, LobbyEventType.TEAM_UPDATE, data);
    }

    public LobbyEvent createReadyUpdateEvent(Long gameId, Long participantId, boolean isReady) {
        LobbyEventData.LobbyReadyUpdateData data = LobbyEventData.LobbyReadyUpdateData.of(participantId, isReady);
        return LobbyEvent.of(gameId, LobbyEventType.READY_UPDATE, data);
    }
}
