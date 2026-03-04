package com.team.cops_and_robbers.game.participant.application.dto.result;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;

import java.util.List;

public record GameParticipantListResult(
        List<ParticipantInfo> police,
        List<ParticipantInfo> robbers
) {
    public record ParticipantInfo(Long participantId, String nickname, String status) {
        public static ParticipantInfo from(GameParticipant participant) {
            return new ParticipantInfo(
                    participant.getId(),
                    participant.getUser().getNickname(),
                    participant.getStatus().name()
            );
        }
    }

    public static GameParticipantListResult from(List<GameParticipant> participants) {
        List<ParticipantInfo> police = participants.stream()
                .filter(GameParticipant::isPolice)
                .map(ParticipantInfo::from)
                .toList();
        List<ParticipantInfo> robbers = participants.stream()
                .filter(GameParticipant::isRobber)
                .map(ParticipantInfo::from)
                .toList();
        return new GameParticipantListResult(police, robbers);
    }
}