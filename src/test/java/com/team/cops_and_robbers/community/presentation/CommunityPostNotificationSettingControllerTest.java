package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.CommunityPostFixture;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.CommunityPostNotificationSetting;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("커뮤니티 게시글별 알림 설정 API")
class CommunityPostNotificationSettingControllerTest extends ControllerTest {

    private static final String SETTING_PATH = "/api/community-posts/{postId}/notification-settings";

    private CommunityPost givenPost(User writer) {
        return communityPostRepository.save(CommunityPostFixture.POST(writer.getId()));
    }

    private CommunityPostNotificationSetting findSetting(CommunityPost post, User user) {
        return communityPostNotificationSettingRepository
                .findByCommunityPostIdAndUserId(post.getId(), user.getId())
                .orElseThrow();
    }

    @Nested
    @DisplayName("게시글 알림 설정 변경")
    class UpdateNotificationSetting {

        @Test
        void 토글을_처음_건드리면_설정이_저장된다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);

            authenticated(givenAccessToken(user))
                    .body(Map.of("notifyComments", false, "notifyReplies", true))
                    .put(SETTING_PATH, post.getId())
                    .then().statusCode(204);

            CommunityPostNotificationSetting setting = findSetting(post, user);
            assertSoftly(softly -> {
                softly.assertThat(setting.isNotifyComments()).isFalse();
                softly.assertThat(setting.isNotifyReplies()).isTrue();
            });
        }

        @Test
        void 다시_호출하면_행을_새로_만들지_않고_값만_바뀐다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);

            authenticated(givenAccessToken(user))
                    .body(Map.of("notifyComments", false, "notifyReplies", true))
                    .put(SETTING_PATH, post.getId())
                    .then().statusCode(204);
            authenticated(givenAccessToken(user))
                    .body(Map.of("notifyComments", true, "notifyReplies", false))
                    .put(SETTING_PATH, post.getId())
                    .then().statusCode(204);

            assertSoftly(softly -> {
                softly.assertThat(communityPostNotificationSettingRepository.count()).isEqualTo(1);
                softly.assertThat(findSetting(post, user).isNotifyComments()).isTrue();
                softly.assertThat(findSetting(post, user).isNotifyReplies()).isFalse();
            });
        }

        @Test
        void 남의_글에도_내_설정을_따로_저장할_수_있다() {
            User writer = givenUser("작성자");
            User reader = givenUser("독자");
            CommunityPost post = givenPost(writer);

            authenticated(givenAccessToken(reader))
                    .body(Map.of("notifyComments", true, "notifyReplies", true))
                    .put(SETTING_PATH, post.getId())
                    .then().statusCode(204);

            assertThat(findSetting(post, reader).isNotifyComments()).isTrue();
        }

        @Test
        void 존재하지_않는_게시글이면_설정할_수_없다() {
            User user = givenUser("유저");

            authenticated(givenAccessToken(user))
                    .body(Map.of("notifyComments", true, "notifyReplies", true))
                    .put(SETTING_PATH, 999)
                    .then()
                    .statusCode(CommunityPostException.POST_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 수신_여부가_빠지면_400을_응답한다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);

            authenticated(givenAccessToken(user))
                    .body(Map.of("notifyComments", true))
                    .put(SETTING_PATH, post.getId())
                    .then().statusCode(400);
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .body(Map.of("notifyComments", true, "notifyReplies", true))
                    .put(SETTING_PATH, 1)
                    .then().statusCode(401);
        }
    }
}
