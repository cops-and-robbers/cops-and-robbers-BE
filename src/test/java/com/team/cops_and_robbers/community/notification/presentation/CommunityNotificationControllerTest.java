package com.team.cops_and_robbers.community.notification.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.community.notification.domain.CommunityNotification;
import com.team.cops_and_robbers.community.notification.domain.CommunityNotificationType;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("커뮤니티 알림함 API")
class CommunityNotificationControllerTest extends ControllerTest {

    private static final String NOTIFICATIONS_PATH = "/api/community-posts/notifications";
    private static final String UNREAD_COUNT_PATH = NOTIFICATIONS_PATH + "/unread-count";
    private static final String READ_PATH = NOTIFICATIONS_PATH + "/read";

    /** 서버 시계가 KST로 고정돼 있어 테스트도 같은 기준으로 시각을 만든다. */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CommunityNotification givenNotification(User user) {
        return communityNotificationRepository.save(CommunityNotification.createNotification(
                user.getId(), CommunityNotificationType.COMMENT, 1L, "같이 하실 분!", "몇 시에 만나나요?"));
    }

    private void givenCreatedAt(CommunityNotification notification, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE community_notifications SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(createdAt),
                notification.getId()
        );
    }

    private Map<String, Object> getNotifications(User user) {
        return authenticated(givenAccessToken(user))
                .get(NOTIFICATIONS_PATH)
                .then().statusCode(200)
                .extract().as(new TypeRef<>() {});
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractContent(Map<String, Object> response) {
        return (List<Map<String, Object>>) response.get("content");
    }

    private int unreadCountOf(User user) {
        Map<String, Object> response = authenticated(givenAccessToken(user))
                .get(UNREAD_COUNT_PATH)
                .then().statusCode(200)
                .extract().as(new TypeRef<>() {});
        return (int) response.get("unreadCount");
    }

    @Nested
    @DisplayName("알림함 목록 조회")
    class GetNotifications {

        @Test
        void 최근에_생긴_알림부터_조회된다() {
            User user = givenUser("유저");
            CommunityNotification older = givenNotification(user);
            CommunityNotification newer = givenNotification(user);

            List<Map<String, Object>> content = extractContent(getNotifications(user));

            assertSoftly(softly -> {
                softly.assertThat(content).hasSize(2);
                softly.assertThat(content.get(0).get("id")).isEqualTo(newer.getId().intValue());
                softly.assertThat(content.get(1).get("id")).isEqualTo(older.getId().intValue());
            });
        }

        @Test
        void 남의_알림은_내_목록에_나오지_않는다() {
            User user = givenUser("유저");
            givenNotification(givenUser("다른유저"));

            assertThat(extractContent(getNotifications(user))).isEmpty();
        }

        @Test
        void 보관_기간이_지난_알림은_조회되지_않는다() {
            User user = givenUser("유저");
            givenCreatedAt(givenNotification(user), LocalDateTime.now(KST).minusDays(61));

            assertThat(extractContent(getNotifications(user))).isEmpty();
        }

        @Test
        void 요청_크기보다_많으면_커서로_다음_페이지를_이어받는다() {
            User user = givenUser("유저");
            for (int i = 0; i < 3; i++) {
                givenNotification(user);
            }

            Map<String, Object> firstPage = authenticated(givenAccessToken(user))
                    .queryParam("size", 2)
                    .get(NOTIFICATIONS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            assertSoftly(softly -> {
                softly.assertThat(extractContent(firstPage)).hasSize(2);
                softly.assertThat(firstPage.get("hasNext")).isEqualTo(true);
            });

            Map<String, Object> secondPage = authenticated(givenAccessToken(user))
                    .queryParam("cursor", firstPage.get("nextCursor"))
                    .queryParam("size", 2)
                    .get(NOTIFICATIONS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            assertSoftly(softly -> {
                softly.assertThat(extractContent(secondPage)).hasSize(1);
                softly.assertThat(secondPage.get("hasNext")).isEqualTo(false);
            });
        }

        @Test
        void 조회만으로는_읽음_처리되지_않는다() {
            User user = givenUser("유저");
            givenNotification(user);

            getNotifications(user);

            assertThat(unreadCountOf(user)).isEqualTo(1);
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .get(NOTIFICATIONS_PATH)
                    .then().statusCode(401);
        }
    }

    @Nested
    @DisplayName("안 읽은 알림 개수 조회")
    class GetUnreadCount {

        @Test
        void 읽은_적이_없으면_보관_기간_안의_알림을_모두_센다() {
            User user = givenUser("유저");
            givenNotification(user);
            givenNotification(user);

            assertThat(unreadCountOf(user)).isEqualTo(2);
        }

        @Test
        void 보관_기간이_지난_알림은_세지_않는다() {
            User user = givenUser("유저");
            givenCreatedAt(givenNotification(user), LocalDateTime.now(KST).minusDays(61));

            assertThat(unreadCountOf(user)).isZero();
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .get(UNREAD_COUNT_PATH)
                    .then().statusCode(401);
        }
    }

    @Nested
    @DisplayName("알림 읽음 처리")
    class ReadNotifications {

        @Test
        void 읽음_처리하면_안_읽은_개수가_0이_된다() {
            User user = givenUser("유저");
            givenNotification(user);

            authenticated(givenAccessToken(user))
                    .post(READ_PATH)
                    .then().statusCode(204);

            assertThat(unreadCountOf(user)).isZero();
        }

        @Test
        void 읽음_처리_뒤에_생긴_알림은_다시_안_읽음이다() {
            User user = givenUser("유저");
            givenNotification(user);

            authenticated(givenAccessToken(user))
                    .post(READ_PATH)
                    .then().statusCode(204);
            givenCreatedAt(givenNotification(user), LocalDateTime.now(KST).plusMinutes(1));

            assertThat(unreadCountOf(user)).isEqualTo(1);
        }

        @Test
        void 목록의_읽음_여부에도_반영된다() {
            User user = givenUser("유저");
            givenNotification(user);

            authenticated(givenAccessToken(user))
                    .post(READ_PATH)
                    .then().statusCode(204);

            assertThat(extractContent(getNotifications(user)).getFirst().get("read")).isEqualTo(true);
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .post(READ_PATH)
                    .then().statusCode(401);
        }
    }
}
