package com.team.cops_and_robbers.game.participant.application.dto.result;

import com.team.cops_and_robbers.game.participant.domain.GameParticipant;

import java.util.List;

public record GameParticipantListResult(
        List<ParticipantInfo> chasers,
        List<ParticipantInfo> runners
) {
    public record ParticipantInfo(Long participantId, String nickname, String status) {
        public static ParticipantInfo from(GameParticipant participant) {
            return new ParticipantInfo(
                    participant.getId(),
                    participant.getUser().getNickname(),
                    participant.getStatus().legacyName()
            );
        }
    }

    public static GameParticipantListResult from(List<GameParticipant> participants) {
        List<ParticipantInfo> chasers = participants.stream()
                .filter(GameParticipant::isChaser)
                .map(ParticipantInfo::from)
                .toList();
        List<ParticipantInfo> runners = participants.stream()
                .filter(GameParticipant::isRunner)
                .map(ParticipantInfo::from)
                .toList();
        return new GameParticipantListResult(chasers, runners);
    }
}