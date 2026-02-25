package com.team.cops_and_robbers.play.lobby.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.lobby.application.dto.result.LobbyInfoResult;

import java.util.List;

public record LobbyInfoResponse(
        Long myParticipantId,
        Long hostParticipantId,
        Integer maxParticipants,
        Integer locationRevealIntervalMinutes,
        List<ParticipantResponse> participants
) {
    public record ParticipantResponse(
            Long participantId,
            String nickname,
            Team team,
            boolean isReady
    ) {
    }

    public static LobbyInfoResponse from(LobbyInfoResult result) {
        List<ParticipantResponse> participantResponses = result.participants().stream()
                .map(participantInfo -> new ParticipantResponse(
                        participantInfo.participantId(),
                        participantInfo.nickname(),
                        participantInfo.team(),
                        participantInfo.isReady()
                ))
                .toList();
        return new LobbyInfoResponse(
                result.myParticipantId(),
                result.hostParticipantId(),
                result.maxParticipants(),
                result.locationRevealIntervalMinutes(),
                participantResponses
        );
    }
}
