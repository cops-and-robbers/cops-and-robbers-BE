package com.team.cops_and_robbers.auth.presentation.dto.request;

import com.team.cops_and_robbers.auth.application.dto.command.LoginCommand;
import com.team.cops_and_robbers.user.domain.DeviceType;
import com.team.cops_and_robbers.user.domain.SocialType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(

        @Schema(description = "소셜 플랫폼", example = "KAKAO")
        @NotNull(message = "소셜 플랫폼 정보는 필수입니다.")
        SocialType socialPlatform,

        @Schema(description = "소셜 인증 ID 토큰", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        @NotBlank(message = "소셜 인증 토큰(ID Token)은 필수입니다.")
        String idToken,

        @Schema(description = "FCM 디바이스 토큰", example = "fcm-device-token-here")
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String fcmToken,

        @Schema(description = "기기 타입", example = "IOS")
        @NotNull(message = "기기 타입(IOS/ANDROID)은 필수입니다.")
        DeviceType deviceType,

        @Schema(description = "디바이스 고유 ID", example = "unique-device-id-123")
        @NotBlank(message = "디바이스 고유 ID는 필수입니다.")
        String deviceId
) {
    public LoginCommand toCommand() {
        return new LoginCommand(
                this.socialPlatform,
                this.idToken,
                this.fcmToken,
                this.deviceType,
                this.deviceId
        );
    }
}
