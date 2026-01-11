package com.team.cops_and_robbers.auth.presentation.dto.response;

import com.team.cops_and_robbers.auth.domain.Tokens;

public record ReissueResponse(
        Tokens tokens
) {
    public static ReissueResponse from(Tokens tokens) {
        return new ReissueResponse(tokens);
    }
}
