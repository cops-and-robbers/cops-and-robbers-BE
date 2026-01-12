package com.team.cops_and_robbers.auth.application.dto.command;

import com.team.cops_and_robbers.user.domain.DeviceType;
import com.team.cops_and_robbers.user.domain.SocialType;

public record LoginCommand(
        SocialType socialType,
        String idToken,
        String fcmToken,
        DeviceType deviceType,
        String deviceId
) {
}
