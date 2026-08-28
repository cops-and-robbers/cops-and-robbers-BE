
package com.team.cops_and_robbers.community.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class CommunityPostNotificationSettingTest {

    @Nested
    @DisplayName("게시글별 알림 설정 생성")
    class CreateSetting {

        @Test
        void 토글을_건드린_시점의_값을_그대로_담는다() {
            CommunityPostNotificationSetting setting =
                    CommunityPostNotificationSetting.createSetting(1L, 10L, true, false);

            assertSoftly(softly -> {
                softly.assertThat(setting.getUserId()).isEqualTo(1L);
                softly.assertThat(setting.getCommunityPostId()).isEqualTo(10L);
                softly.assertThat(setting.isCommentNotificationsEnabled()).isTrue();
                softly.assertThat(setting.isReplyNotificationsEnabled()).isFalse();
            });
        }
    }

    @Nested
    @DisplayName("게시글별 알림 설정 변경")
    class UpdateSetting {

        @Test
        void 댓글_답글_알림_여부를_한_번에_바꾼다() {
            CommunityPostNotificationSetting setting =
                    CommunityPostNotificationSetting.createSetting(1L, 10L, true, false);

            setting.updateSetting(false, true);

            assertSoftly(softly -> {
                softly.assertThat(setting.isCommentNotificationsEnabled()).isFalse();
                softly.assertThat(setting.isReplyNotificationsEnabled()).isTrue();
            });
        }
    }
}
