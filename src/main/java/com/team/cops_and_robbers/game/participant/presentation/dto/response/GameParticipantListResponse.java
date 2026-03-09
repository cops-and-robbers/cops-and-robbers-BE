package com.team.cops_and_robbers.game.participant.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.application.dto.result.GameParticipantListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GameParticipantListResponse(
        @Schema(description = "경찰 참가자 목록")
        List<ParticipantResponse> police,
        @Schema(description = "도둑 참가자 목록")
        List<ParticipantResponse> robbers
) {
    public record ParticipantResponse(
            @Schema(description = "참가자 ID", example = "1")
            Long participantId,
            @Schema(description = "닉네임", example = "경찰1")
            String nickname,
            @Schema(description = "상태", example = "POLICE_WAITING")
            String status
    ) {}

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