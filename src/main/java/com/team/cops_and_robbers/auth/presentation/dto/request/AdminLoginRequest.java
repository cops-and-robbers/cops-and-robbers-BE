package com.team.cops_and_robbers.auth.presentation.dto.request;

import com.team.cops_and_robbers.auth.application.dto.command.AdminLoginCommand;
import com.team.cops_and_robbers.user.domain.SocialType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminLoginRequest(

        @Schema(description = "소셜 플랫폼", example = "GOOGLE")
        @NotNull(message = "소셜 플랫폼 정보는 필수입니다.")
        SocialType socialPlatform,

        @Schema(description = "소셜 인증 ID 토큰", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        @NotBlank(message = "소셜 인증 토큰(ID Token)은 필수입니다.")
        String idToken
) {
    public AdminLoginCommand toCommand() {
        return new AdminLoginCommand(socialPlatform, idToken);
    }
}
