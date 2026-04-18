package com.team.cops_and_robbers.user.domain;

import com.team.cops_and_robbers.common.fixture.UserDeviceFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class UserDeviceTest {

    @Nested
    @DisplayName("기기 정보 업데이트 (reconnect)")
    class Reconnect {

        @Test
        void 기존_기기_정보와_다른_정보가_들어오면_정상적으로_업데이트된다() {
            // given
            User user = UserFixture.USER();
            UserDevice userDevice = UserDeviceFixture.IOS_DEVICE(user);
            
            String newDeviceId = "new_device_123";
            DeviceType newDeviceType = DeviceType.ANDROID;
            String newFcmToken = "new_fcm_token";

            // when
            userDevice.reconnect(newDeviceId, newDeviceType, newFcmToken);

            // then
            assertSoftly(softly -> {
                softly.assertThat(userDevice.getDeviceId()).isEqualTo(newDeviceId);
                softly.assertThat(userDevice.getDeviceType()).isEqualTo(newDeviceType);
                softly.assertThat(userDevice.getFcmToken()).isEqualTo(newFcmToken);
            });
        }

        @Test
        void fcmToken에_null이_들어오면_기존_토큰을_안전하게_유지한다() {
            // given
            User user = UserFixture.USER();
            UserDevice userDevice = UserDeviceFixture.IOS_DEVICE(user);
            String oldFcmToken = userDevice.getFcmToken();
            
            String newDeviceId = "new_device_123";

            // when
            userDevice.reconnect(newDeviceId, userDevice.getDeviceType(), null);

            // then
            assertSoftly(softly -> {
                softly.assertThat(userDevice.getDeviceId()).isEqualTo(newDeviceId);
                softly.assertThat(userDevice.getFcmToken()).isEqualTo(oldFcmToken);
            });
        }
    }
}
