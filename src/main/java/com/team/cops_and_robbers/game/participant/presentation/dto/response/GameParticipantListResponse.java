package com.team.cops_and_robbers.game.participant.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.application.dto.result.GameParticipantListResult;

import java.util.List;

public record GameParticipantListResponse(
        List<ParticipantResponse> police,
        List<ParticipantResponse> robbers
) {
    public record ParticipantResponse(Long participantId, String nickname, String status) {}

    public static GameParticipantListResponse from(GameParticipantListResult result) {
        List<ParticipantResponse> police = result.police().stream()
                .map(p -> new ParticipantResponse(p.participantId(), p.nickname(), p.status()))
                .toList();
        List<ParticipantResponse> robbers = result.robbers().stream()
                .map(p -> new ParticipantResponse(p.participantId(), p.nickname(), p.status()))
                .toList();
        return new GameParticipantListResponse(police, robbers);
    }
}