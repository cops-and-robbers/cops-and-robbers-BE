package com.team.cops_and_robbers.auth.presentation.dto.response;

import com.team.cops_and_robbers.auth.domain.Tokens;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReissueResponse(
        @Schema(description = "JWT 토큰 정보")
        Tokens tokens
) {
    public static ReissueResponse from(Tokens tokens) {
        return new ReissueResponse(tokens);
    }
}
