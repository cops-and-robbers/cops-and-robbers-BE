package com.team.cops_and_robbers.play.lobby.presentation.dto.response;

import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.lobby.application.dto.result.LobbyInfoResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record LobbyInfoResponse(
        @Schema(description = "내 참가자 ID", example = "2")
        Long myParticipantId,
        @Schema(description = "방장 참가자 ID", example = "1")
        Long hostParticipantId,
        @Schema(description = "초대 코드", example = "ABC123")
        String inviteCode,
        @Schema(description = "참가자 목록")
        List<LobbyParticipantResponse> participants
) {
    @Schema(name = "LobbyParticipantResponse", description = "로비 참가자 정보")
    public record LobbyParticipantResponse(
            @Schema(description = "참가자 ID", example = "1")
            Long participantId,
            @Schema(description = "닉네임", example = "방장닉네임")
            String nickname,
            @Schema(description = "팀", example = "POLICE")
            Team team,
            @Schema(description = "준비 여부", example = "true")
            boolean isReady
    ) {
    }

    public static LobbyInfoResponse from(LobbyInfoResult result) {
        List<LobbyParticipantResponse> participantResponses = result.participants().stream()
                .map(participantInfo -> new LobbyParticipantResponse(
                        participantInfo.participantId(),
                        participantInfo.nickname(),
                        participantInfo.team(),
                        participantInfo.isReady()
                ))
                .toList();
        return new LobbyInfoResponse(
                result.myParticipantId(),
                result.hostParticipantId(),
                result.inviteCode(),
                participantResponses
        );
    }
}
