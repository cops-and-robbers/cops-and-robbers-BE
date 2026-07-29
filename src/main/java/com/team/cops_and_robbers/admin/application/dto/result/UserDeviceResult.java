package com.team.cops_and_robbers.admin.application.dto.result;

import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.user.domain.DeviceType;
import com.team.cops_and_robbers.user.domain.UserDevice;

public record UserDeviceResult(
        DeviceType deviceType,
        String createdAt
) {
    public static UserDeviceResult from(UserDevice userDevice) {
        return new UserDeviceResult(
                userDevice.getDeviceType(),
                TimestampUtil.toIsoString(userDevice.getCreatedAt())
        );
    }
}
