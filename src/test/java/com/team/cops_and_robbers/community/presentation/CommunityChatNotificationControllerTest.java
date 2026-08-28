package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.CommunityPostFixture;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("커뮤니티 채팅방 알림·읽음 API")
class CommunityChatNotificationControllerTest extends ControllerTest {

    private static final String ROOMS_PATH = "/api/community-posts/chat/rooms";
    private static final String NOTIFICATION_PATH = "/api/community-posts/{postId}/chat/notification";
    private static final String READ_PATH = "/api/community-posts/{postId}/chat/read";
    private static final String MEMBERS_PATH = "/api/community-posts/{postId}/chat/members";

    private CommunityPost givenChatRoom(User writer) {
        CommunityPost post = communityPostRepository.save(CommunityPostFixture.POST(writer.getId()));
        communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), writer.getId()));
        return post;
    }

    private void givenMember(CommunityPost post, User user) {
        communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), user.getId()));
    }

    private CommunityChatMessage givenMessage(CommunityPost post, User sender, CommunityChatMessageType type) {
        return communityChatMessageRepository.save(CommunityChatMessage.createMessage(
                UUID.randomUUID().toString(), post.getId(), sender.getId(), sender.getNickname(),
                sender.getProfileIcon(), "다들 오셨나요", type));
    }

    private boolean notificationEnabledOf(User user, CommunityPost post) {
        return authenticated(givenAccessToken(user))
                .get(MEMBERS_PATH, post.getId())
                .then().statusCode(200)
                .extract().jsonPath().getBoolean("notificationEnabled");
    }

    private Map<String, Object> findRoom(User user, CommunityPost post) {
        Map<String, Object> response = authenticated(givenAccessToken(user))
                .get(ROOMS_PATH)
                .then().statusCode(200)
                .extract().as(new TypeRef<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rooms = (List<Map<String, Object>>) response.get("chatRooms");
        return rooms.stream()
                .filter(room -> room.get("postId").equals(post.getId().intValue()))
                .findFirst()
                .orElseThrow();
    }

    private boolean allowNotificationOf(CommunityPost post, User user) {
        return communityChatMemberRepository
                .findByCommunityPostIdAndUserId(post.getId(), user.getId())
                .orElseThrow()
                .isAllowNotification();
    }

    @Nested
    @DisplayName("채팅방 알림 설정")
    class UpdateNotification {

        @Test
        void 기본값은_알림_받음이다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);

            assertSoftly(softly -> {
                softly.assertThat(allowNotificationOf(post, writer)).isTrue();
                softly.assertThat(notificationEnabledOf(writer, post)).isTrue();
            });
        }

        @Test
        void 끄면_멤버_조회에도_반영된다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);

            authenticated(givenAccessToken(writer))
                    .body(Map.of("allowNotification", false))
                    .put(NOTIFICATION_PATH, post.getId())
                    .then().statusCode(204);

            assertSoftly(softly -> {
                softly.assertThat(allowNotificationOf(post, writer)).isFalse();
                softly.assertThat(notificationEnabledOf(writer, post)).isFalse();
            });
        }

        @Test
        void 껐다가_다시_켤_수_있다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);

            authenticated(givenAccessToken(writer))
                    .body(Map.of("allowNotification", false))
                    .put(NOTIFICATION_PATH, post.getId())
                    .then().statusCode(204);
            authenticated(givenAccessToken(writer))
                    .body(Map.of("allowNotification", true))
                    .put(NOTIFICATION_PATH, post.getId())
                    .then().statusCode(204);

            assertThat(allowNotificationOf(post, writer)).isTrue();
        }

        @Test
        void 참여하지_않은_방은_설정할_수_없다() {
            User writer = givenUser("작성자");
            User stranger = givenUser("남");
            CommunityPost post = givenChatRoom(writer);

            authenticated(givenAccessToken(stranger))
                    .body(Map.of("allowNotification", false))
                    .put(NOTIFICATION_PATH, post.getId())
                    .then()
                    .statusCode(CommunityChatException.NOT_A_CHAT_MEMBER.getHttpStatus().value());
        }

        @Test
        void 수신_여부가_빠지면_400을_응답한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);

            authenticated(givenAccessToken(writer))
                    .body(Map.of())
                    .put(NOTIFICATION_PATH, post.getId())
                    .then().statusCode(400);
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .body(Map.of("allowNotification", false))
                    .put(NOTIFICATION_PATH, 1)
                    .then().statusCode(401);
        }
    }

    @Nested
    @DisplayName("안 읽은 메시지 수")
    class UnreadCount {

        @Test
        void 한_번도_안_읽었으면_남이_보낸_메시지를_모두_센다() {
            User writer = givenUser("작성자");
            User other = givenUser("참여자");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, other);
            givenMessage(post, other, CommunityChatMessageType.TEXT);
            givenMessage(post, other, CommunityChatMessageType.TEXT);

            assertThat(findRoom(writer, post).get("unreadCount")).isEqualTo(2);
        }

        @Test
        void 내가_보낸_메시지는_세지_않는다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);
            givenMessage(post, writer, CommunityChatMessageType.TEXT);

            assertThat(findRoom(writer, post).get("unreadCount")).isEqualTo(0);
        }

        @Test
        void 입장_퇴장_안내는_세지_않는다() {
            User writer = givenUser("작성자");
            User other = givenUser("참여자");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, other);
            givenMessage(post, other, CommunityChatMessageType.SYSTEM);

            assertThat(findRoom(writer, post).get("unreadCount")).isEqualTo(0);
        }

        @Test
        void 알림을_꺼도_안_읽은_개수는_그대로_오른다() {
            User writer = givenUser("작성자");
            User other = givenUser("참여자");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, other);
            authenticated(givenAccessToken(writer))
                    .body(Map.of("allowNotification", false))
                    .put(NOTIFICATION_PATH, post.getId())
                    .then().statusCode(204);

            givenMessage(post, other, CommunityChatMessageType.TEXT);

            Map<String, Object> room = findRoom(writer, post);
            assertSoftly(softly -> {
                softly.assertThat(notificationEnabledOf(writer, post)).isFalse();
                softly.assertThat(room.get("unreadCount")).isEqualTo(1);
            });
        }
    }

    @Nested
    @DisplayName("채팅방 읽음 처리")
    class Read {

        @Test
        void 읽음_처리하면_안_읽은_개수가_0이_된다() {
            User writer = givenUser("작성자");
            User other = givenUser("참여자");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, other);
            CommunityChatMessage last = givenMessage(post, other, CommunityChatMessageType.TEXT);

            authenticated(givenAccessToken(writer))
                    .body(Map.of("lastReadMessageId", last.getId()))
                    .post(READ_PATH, post.getId())
                    .then().statusCode(204);

            assertThat(findRoom(writer, post).get("unreadCount")).isEqualTo(0);
        }

        @Test
        void 읽음_처리_뒤에_온_메시지는_다시_안_읽음이_된다() {
            User writer = givenUser("작성자");
            User other = givenUser("참여자");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, other);
            CommunityChatMessage last = givenMessage(post, other, CommunityChatMessageType.TEXT);
            authenticated(givenAccessToken(writer))
                    .body(Map.of("lastReadMessageId", last.getId()))
                    .post(READ_PATH, post.getId())
                    .then().statusCode(204);

            givenMessage(post, other, CommunityChatMessageType.TEXT);

            assertThat(findRoom(writer, post).get("unreadCount")).isEqualTo(1);
        }

        @Test
        void 과거_메시지_id를_보내도_읽음_위치가_뒤로_밀리지_않는다() {
            User writer = givenUser("작성자");
            User other = givenUser("참여자");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, other);
            CommunityChatMessage older = givenMessage(post, other, CommunityChatMessageType.TEXT);
            CommunityChatMessage newer = givenMessage(post, other, CommunityChatMessageType.TEXT);
            authenticated(givenAccessToken(writer))
                    .body(Map.of("lastReadMessageId", newer.getId()))
                    .post(READ_PATH, post.getId())
                    .then().statusCode(204);

            authenticated(givenAccessToken(writer))
                    .body(Map.of("lastReadMessageId", older.getId()))
                    .post(READ_PATH, post.getId())
                    .then().statusCode(204);

            assertThat(findRoom(writer, post).get("unreadCount")).isEqualTo(0);
        }

        @Test
        void 방의_마지막_메시지보다_큰_id를_보내도_이후_메시지는_안_읽음으로_남는다() {
            User writer = givenUser("작성자");
            User other = givenUser("참여자");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, other);
            givenMessage(post, other, CommunityChatMessageType.TEXT);

            authenticated(givenAccessToken(writer))
                    .body(Map.of("lastReadMessageId", Long.MAX_VALUE))
                    .post(READ_PATH, post.getId())
                    .then().statusCode(204);

            givenMessage(post, other, CommunityChatMessageType.TEXT);

            assertThat(findRoom(writer, post).get("unreadCount")).isEqualTo(1);
        }

        @Test
        void 참여하지_않은_방은_읽음_처리할_수_없다() {
            User writer = givenUser("작성자");
            User stranger = givenUser("남");
            CommunityPost post = givenChatRoom(writer);

            authenticated(givenAccessToken(stranger))
                    .body(Map.of("lastReadMessageId", 1))
                    .post(READ_PATH, post.getId())
                    .then()
                    .statusCode(CommunityChatException.NOT_A_CHAT_MEMBER.getHttpStatus().value());
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .body(Map.of("lastReadMessageId", 1))
                    .post(READ_PATH, 1)
                    .then().statusCode(401);
        }
    }
}
