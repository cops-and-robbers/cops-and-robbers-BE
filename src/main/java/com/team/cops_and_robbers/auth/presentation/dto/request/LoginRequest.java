package com.team.cops_and_robbers.auth.presentation.dto.request;

import com.team.cops_and_robbers.auth.application.dto.command.LoginCommand;
import com.team.cops_and_robbers.user.domain.DeviceType;
import com.team.cops_and_robbers.user.domain.SocialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(

        @NotNull(message = "소셜 플랫폼 정보는 필수입니다.")
        SocialType socialPlatform,

        @NotBlank(message = "소셜 인증 토큰(ID Token)은 필수입니다.")
        String idToken,

        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String fcmToken,

        @NotNull(message = "기기 타입(IOS/ANDROID)은 필수입니다.")
        DeviceType deviceType,

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
