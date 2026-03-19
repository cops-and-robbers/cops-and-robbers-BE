package com.team.cops_and_robbers.play.lobby.application;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.game.game.application.dto.result.GameAreaUpdateResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameSettingsUpdateResult;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEventData;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEventType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    public LobbyEvent createReadyUpdateEvent(Long gameId, GameParticipant participant) {
        LobbyEventData.LobbyReadyUpdateData data = LobbyEventData.LobbyReadyUpdateData.from(participant);
        return LobbyEvent.of(gameId, LobbyEventType.READY_UPDATE, data);
    }

    public LobbyEvent createHostChangedEvent(Long gameId, GameParticipant newHost) {
        LobbyEventData.LobbyHostChangedData data = LobbyEventData.LobbyHostChangedData.from(newHost);
        return LobbyEvent.of(gameId, LobbyEventType.HOST_CHANGED, data);
    }

    public LobbyEvent createGameStartEvent(Long gameId, LocalDateTime startedAt) {
        String startTimeStr = TimestampUtil.toIsoString(startedAt);
        LobbyEventData.LobbyGameStartData data = LobbyEventData.LobbyGameStartData.of(startTimeStr);
        return LobbyEvent.of(gameId, LobbyEventType.GAME_START, data);
    }

    public LobbyEvent createAreaUpdatedEvent(Long gameId, GameAreaUpdateResult result) {
        LobbyEventData.LobbyAreaUpdatedData data = LobbyEventData.LobbyAreaUpdatedData.from(result);
        return LobbyEvent.of(gameId, LobbyEventType.AREA_UPDATED, data);
    }

    public LobbyEvent createSettingsUpdatedEvent(Long gameId, GameSettingsUpdateResult result) {
        LobbyEventData.LobbySettingsUpdatedData data = LobbyEventData.LobbySettingsUpdatedData.from(result);
        return LobbyEvent.of(gameId, LobbyEventType.SETTINGS_UPDATED, data);
    }

    public LobbyEvent createKickEvent(Long gameId, Long kickedParticipantId, String nickname) {
        LobbyEventData.LobbyKickedData data = LobbyEventData.LobbyKickedData.of(kickedParticipantId, nickname);
        return LobbyEvent.of(gameId, LobbyEventType.KICKED, data);
    }
}
