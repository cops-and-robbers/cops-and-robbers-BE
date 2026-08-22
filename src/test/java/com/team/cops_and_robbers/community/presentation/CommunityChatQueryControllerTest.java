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

@DisplayName("커뮤니티 채팅 조회 API")
class CommunityChatQueryControllerTest extends ControllerTest {

    private static final String HISTORY_PATH = "/api/community-posts/{postId}/chat/messages";
    private static final String ROOMS_PATH = "/api/community-posts/chat/rooms";

    private CommunityPost givenChatRoom(User member) {
        CommunityPost post = communityPostRepository.save(CommunityPostFixture.POST(member.getId()));
        communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), member.getId()));
        return post;
    }

    private CommunityChatMessage givenMessage(CommunityPost post, User sender, String message) {
        return communityChatMessageRepository.save(CommunityChatMessage.createMessage(
                UUID.randomUUID().toString(), post.getId(), sender.getId(), sender.getNickname(),
                message, CommunityChatMessageType.TEXT));
    }

    @Nested
    @DisplayName("채팅 내역 조회")
    class GetHistory {

        @Test
        void 최신순으로_조회하고_커서로_다음_페이지를_이어받는다() {
            User member = givenUser("member");
            CommunityPost post = givenChatRoom(member);
            for (int i = 1; i <= 5; i++) {
                givenMessage(post, member, "메시지" + i);
            }

            Map<String, Object> firstPage = authenticated(givenAccessToken(member))
                    .queryParam("size", 3)
                    .get(HISTORY_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            List<Map<String, Object>> messages = extractMessages(firstPage);
            assertThat(messages).hasSize(3);
            assertThat(messages.get(0).get("message")).isEqualTo("메시지5");
            assertThat(messages.get(2).get("message")).isEqualTo("메시지3");
            assertThat(firstPage.get("hasNext")).isEqualTo(true);

            Map<String, Object> secondPage = authenticated(givenAccessToken(member))
                    .queryParam("cursor", firstPage.get("nextCursor"))
                    .queryParam("size", 3)
                    .get(HISTORY_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            List<Map<String, Object>> remaining = extractMessages(secondPage);
            assertThat(remaining).hasSize(2);
            assertThat(remaining.get(0).get("message")).isEqualTo("메시지2");
            assertThat(secondPage.get("hasNext")).isEqualTo(false);
        }

        @Test
        void 닉네임을_변경하면_과거_메시지도_새_닉네임으로_조회된다() {
            User member = givenUser("변경전");
            CommunityPost post = givenChatRoom(member);
            givenMessage(post, member, "안녕하세요");

            member.updateNickname("변경후");
            userRepository.save(member);

            Map<String, Object> response = authenticated(givenAccessToken(member))
                    .get(HISTORY_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            assertThat(extractMessages(response).get(0).get("senderNickname")).isEqualTo("변경후");
        }

        @Test
        void 채팅방_멤버가_아니면_조회할_수_없다() {
            User member = givenUser("member");
            CommunityPost post = givenChatRoom(member);
            User outsider = givenUser("outsider");

            authenticated(givenAccessToken(outsider))
                    .get(HISTORY_PATH, post.getId())
                    .then()
                    .statusCode(CommunityChatException.NOT_A_CHAT_MEMBER.getHttpStatus().value());
        }

        @Test
        void 조회_개수가_허용_범위를_벗어나면_400을_반환한다() {
            User member = givenUser("member");
            CommunityPost post = givenChatRoom(member);

            authenticated(givenAccessToken(member))
                    .queryParam("size", 51)
                    .get(HISTORY_PATH, post.getId())
                    .then().statusCode(400);
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> extractMessages(Map<String, Object> response) {
            return (List<Map<String, Object>>) response.get("messages");
        }
    }

    @Nested
    @DisplayName("내 채팅방 목록 조회")
    class GetChatRooms {

        @Test
        void 마지막_대화가_최근인_방부터_조회한다() {
            User member = givenUser("member");
            CommunityPost older = givenChatRoom(member);
            CommunityPost newer = givenChatRoom(member);

            givenMessage(older, member, "오래된 대화");
            givenMessage(newer, member, "최근 대화");

            Map<String, Object> response = authenticated(givenAccessToken(member))
                    .get(ROOMS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            List<Map<String, Object>> rooms = extractRooms(response);
            assertThat(rooms).hasSize(2);
            assertThat(rooms.get(0).get("postId")).isEqualTo(newer.getId().intValue());
            assertThat(extractLastMessage(rooms.get(0)).get("message")).isEqualTo("최근 대화");
            assertThat(rooms.get(1).get("postId")).isEqualTo(older.getId().intValue());
        }

        @Test
        void 메시지가_없는_방도_lastMessage가_null로_포함된다() {
            User member = givenUser("member");
            CommunityPost post = givenChatRoom(member);

            Map<String, Object> response = authenticated(givenAccessToken(member))
                    .get(ROOMS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            List<Map<String, Object>> rooms = extractRooms(response);
            assertThat(rooms).hasSize(1);
            assertThat(rooms.get(0).get("postId")).isEqualTo(post.getId().intValue());
            assertThat(rooms.get(0).get("lastMessage")).isNull();
            assertThat(rooms.get(0).get("memberCount")).isEqualTo(1);
        }

        @Test
        void 모임_날짜가_지난_방은_ENDED로_조회된다() {
            User member = givenUser("member");
            CommunityPost post = communityPostRepository.save(CommunityPostFixture.PAST_POST(member.getId()));
            communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), member.getId()));

            Map<String, Object> response = authenticated(givenAccessToken(member))
                    .get(ROOMS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            assertThat(extractRooms(response).getFirst().get("status")).isEqualTo("ENDED");
        }

        @Test
        void 참여하지_않은_방은_목록에_나오지_않는다() {
            User member = givenUser("member");
            givenChatRoom(member);

            User other = givenUser("other");
            givenChatRoom(other);

            Map<String, Object> response = authenticated(givenAccessToken(member))
                    .get(ROOMS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            assertThat(extractRooms(response)).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> extractRooms(Map<String, Object> response) {
            return (List<Map<String, Object>>) response.get("chatRooms");
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> extractLastMessage(Map<String, Object> room) {
            return (Map<String, Object>) room.get("lastMessage");
        }
    }
}
