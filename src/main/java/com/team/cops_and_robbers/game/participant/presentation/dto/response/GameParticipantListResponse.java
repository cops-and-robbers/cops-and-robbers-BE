package com.team.cops_and_robbers.game.participant.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.cops_and_robbers.game.participant.application.dto.result.GameParticipantListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GameParticipantListResponse(
        @Schema(description = "경찰 참가자 목록")
        List<InGameParticipantResponse> chasers,
        @Schema(description = "도둑 참가자 목록")
        List<InGameParticipantResponse> runners
) {
    // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonProperty(value = "police", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "옛 이름. chasers 을 사용하세요", deprecated = true)
    public List<InGameParticipantResponse> police() {
        return chasers;
    }

    // 옛 앱 호환: 옛 이름으로도 내보낸다. 강제 업데이트 후 제거
    @Deprecated
    @JsonProperty(value = "robbers", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "옛 이름. runners 을 사용하세요", deprecated = true)
    public List<InGameParticipantResponse> robbers() {
        return runners;
    }

    @Schema(name = "InGameParticipantResponse", description = "인게임 참가자 정보")
    public record InGameParticipantResponse(
            @Schema(description = "참가자 ID", example = "1")
            Long participantId,
            @Schema(description = "닉네임", example = "경찰1")
            String nickname,
            @Schema(description = "상태", example = "POLICE_WAITING")
            String status
    ) {}

    public static GameParticipantListResponse from(GameParticipantListResult result) {
        List<InGameParticipantResponse> chasers = result.chasers().stream()
                .map(p -> new InGameParticipantResponse(p.participantId(), p.nickname(), p.status()))
                .toList();
        List<InGameParticipantResponse> runners = result.runners().stream()
                .map(p -> new InGameParticipantResponse(p.participantId(), p.nickname(), p.status()))
                .toList();
        return new GameParticipantListResponse(chasers, runners);
    }
}
