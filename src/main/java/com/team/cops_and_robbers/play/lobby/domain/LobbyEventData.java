package com.team.cops_and_robbers.play.lobby.domain;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;

public sealed interface LobbyEventData permits
        LobbyEventData.LobbyEnterData,
        LobbyEventData.LobbyExitData,
        LobbyEventData.LobbyTeamUpdateData,
        LobbyEventData.LobbyReadyUpdateData,
        LobbyEventData.LobbyGameStartData {

    record LobbyParticipantInfo(Long participantId, String nickname, Team team, boolean isReady) {
        public static LobbyParticipantInfo from(GameParticipant participant) {
            return new LobbyParticipantInfo(
                    participant.getId(),
                    participant.getUser().getNickname(),
                    participant.getTeam(),
                    participant.isReady()
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

    record LobbyTeamUpdateData(LobbyParticipantInfo participant, int policeCount,
                               int robberCount) implements LobbyEventData {
        public static LobbyTeamUpdateData of(GameParticipant participant, int policeCount, int robberCount) {
            return new LobbyTeamUpdateData(LobbyParticipantInfo.from(participant), policeCount, robberCount);
        }
    }

    record LobbyReadyUpdateData(LobbyParticipantInfo participant) implements LobbyEventData {
        public static LobbyReadyUpdateData from(GameParticipant participant) {
            return new LobbyReadyUpdateData(LobbyParticipantInfo.from(participant));
        }
    }

    record LobbyGameStartData(String message, String startTime) implements LobbyEventData {
        public static LobbyGameStartData of(String message, String startTime) {
            return new LobbyGameStartData(message, startTime);
        }
    }
}
