package com.team.cops_and_robbers.play.lobby.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.game.application.dto.result.GameAreaUpdateResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameModeUpdateResult;
import com.team.cops_and_robbers.game.game.application.dto.result.GameSettingsUpdateResult;
import com.team.cops_and_robbers.game.game.domain.GameMode;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;

public sealed interface LobbyEventData permits
        LobbyEventData.LobbyEnterData,
        LobbyEventData.LobbyExitData,
        LobbyEventData.LobbyTeamUpdateData,
        LobbyEventData.LobbyHostChangedData,
        LobbyEventData.LobbyGameStartData,
        LobbyEventData.LobbyAreaUpdatedData,
        LobbyEventData.LobbySettingsUpdatedData,
        LobbyEventData.LobbyModeUpdatedData,
        LobbyEventData.LobbyKickedData {

    record LobbyParticipantInfo(Long participantId, String nickname, Team team) {
        public static LobbyParticipantInfo from(GameParticipant participant) {
            return new LobbyParticipantInfo(
                    participant.getId(),
                    participant.getUser().getNickname(),
                    participant.getTeam()
            );
        }
    }

    record LobbyEnterData(LobbyParticipantInfo newParticipant, int currentCount,
                          int maxCount) implements LobbyEventData {
        public static LobbyEnterData of(GameParticipant participant, int currentCount, int maxCount) {
            return new LobbyEnterData(LobbyParticipantInfo.from(participant), currentCount, maxCount);
        }
    }

    record LobbyExitData(Long exitedParticipantId, int currentCount, int maxCount) implements LobbyEventData {
        public static LobbyExitData of(Long exitedParticipantId, int currentCount, int maxCount) {
            return new LobbyExitData(exitedParticipantId, currentCount, maxCount);
        }
    }

    record LobbyTeamUpdateData(LobbyParticipantInfo participant, int chaserCount,
                               int runnerCount) implements LobbyEventData {
        public static LobbyTeamUpdateData of(GameParticipant participant, int chaserCount, int runnerCount) {
            return new LobbyTeamUpdateData(LobbyParticipantInfo.from(participant), chaserCount, runnerCount);
        }
    }

    record LobbyHostChangedData(LobbyParticipantInfo newHost) implements LobbyEventData {
        public static LobbyHostChangedData from(GameParticipant participant) {
            return new LobbyHostChangedData(LobbyParticipantInfo.from(participant));
        }
    }

    record LobbyGameStartData(String startTime) implements LobbyEventData {
        public static LobbyGameStartData of(String startTime) {
            return new LobbyGameStartData(startTime);
        }
    }

    record LobbyAreaUpdatedData(GameAreaData areaData) implements LobbyEventData {
        public static LobbyAreaUpdatedData from(GameAreaUpdateResult result) {
            return new LobbyAreaUpdatedData(result.areaData());
        }
    }

    record LobbyModeUpdatedData(GameMode gameMode, GameAreaData areaData) implements LobbyEventData {
        public static LobbyModeUpdatedData from(GameModeUpdateResult result) {
            return new LobbyModeUpdatedData(result.gameMode(), result.area().areaData());
        }
    }

    record LobbySettingsUpdatedData(
            GameMode gameMode,
            Integer roundDurationMinutes,
            Integer locationRevealIntervalMinutes,
            Integer chaserWaitMinutes,
            Integer maxParticipants,
            Integer shrinkFinalRadiusInMeters
    ) implements LobbyEventData {
        // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
        @Deprecated
        @JsonProperty(value = "policeWaitMinutes", access = JsonProperty.Access.READ_ONLY)
        public Integer policeWaitMinutes() {
            return chaserWaitMinutes;
        }

        public static LobbySettingsUpdatedData from(GameSettingsUpdateResult result) {
            return new LobbySettingsUpdatedData(
                    result.gameMode(),
                    result.roundDurationMinutes(), result.locationRevealIntervalMinutes(),
                    result.chaserWaitMinutes(), result.maxParticipants(),
                    result.shrinkFinalRadiusInMeters()
            );
        }
    }

    record LobbyKickedData(Long kickedParticipantId, String nickname) implements LobbyEventData {
        public static LobbyKickedData of(Long kickedParticipantId, String nickname) {
            return new LobbyKickedData(kickedParticipantId, nickname);
        }
    }
}
