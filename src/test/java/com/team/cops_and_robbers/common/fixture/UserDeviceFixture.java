package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.user.domain.DeviceType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;

public class UserDeviceFixture {

    public static UserDevice IOS_DEVICE(User user) {
        return UserDevice.builder()
                .user(user)
                .deviceId("123")
                .deviceType(DeviceType.IOS)
                .fcmToken("fcm-123")
                .build();
    }

    public static UserDevice ANDROID_DEVICE(User user) {
        return UserDevice.builder()
                .user(user)
                .deviceId("456")
                .deviceType(DeviceType.ANDROID)
                .fcmToken("fcm-456")
                .build();
    }
}
