package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.CommunityPostFixture;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("커뮤니티 채팅방 고정 채팅 API")
class CommunityChatPinControllerTest extends ControllerTest {

    private static final String PIN_PATH = "/api/community-posts/{postId}/chat/pin";

    private CommunityPost givenChatRoom(User writer) {
        CommunityPost post = communityPostRepository.save(CommunityPostFixture.POST(writer.getId()));
        communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), writer.getId()));
        return post;
    }

    private void givenMember(CommunityPost post, User user) {
        communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), user.getId()));
    }

    private CommunityChatMessage latestMessage(CommunityPost post) {
        List<CommunityChatMessage> messages =
                communityChatMessageRepository.findLatestByPostIdIn(List.of(post.getId()));
        return messages.isEmpty() ? null : messages.getFirst();
    }

    @Nested
    @DisplayName("등록")
    class Register {

        @Test
        void 방장이_등록하면_201과_등록된_내용을_반환한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);

            Map<String, Object> response = authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "정문에서 만나요"))
                    .post(PIN_PATH, post.getId())
                    .then().statusCode(201)
                    .extract().jsonPath().getMap("$");

            assertSoftly(softly -> {
                softly.assertThat(response.get("content")).isEqualTo("정문에서 만나요");
                softly.assertThat(response.get("writerId")).isEqualTo(writer.getId().intValue());
            });
        }

        @Test
        void 방장이_아니면_등록할_수_없다() {
            User writer = givenUser("작성자");
            User stranger = givenUser("남");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, stranger);

            authenticated(givenAccessToken(stranger))
                    .body(Map.of("content", "정문에서 만나요"))
                    .post(PIN_PATH, post.getId())
                    .then()
                    .statusCode(CommunityChatException.FORBIDDEN_NOT_CHAT_PIN_HOST.getHttpStatus().value());
        }

        @Test
        void 내용이_비어있으면_400을_응답한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);

            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", ""))
                    .post(PIN_PATH, post.getId())
                    .then().statusCode(400);
        }

        @Test
        void 재등록하면_이전_내용이_사라진다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);
            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "예전 공지"))
                    .post(PIN_PATH, post.getId())
                    .then().statusCode(201);

            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "새 공지"))
                    .post(PIN_PATH, post.getId())
                    .then().statusCode(201);

            assertSoftly(softly -> {
                softly.assertThat(communityChatPinRepository.findAll()).hasSize(1);
                softly.assertThat(communityChatPinRepository.findByCommunityPostId(post.getId()).orElseThrow().getContent())
                        .isEqualTo("새 공지");
            });
        }

        @Test
        void 등록하면_채팅_히스토리에_SYSTEM_메시지가_남는다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);

            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "정문에서 만나요"))
                    .post(PIN_PATH, post.getId())
                    .then().statusCode(201);

            CommunityChatMessage last = latestMessage(post);
            assertSoftly(softly -> {
                softly.assertThat(last).isNotNull();
                softly.assertThat(last.getMessageType()).isEqualTo(CommunityChatMessageType.SYSTEM);
                softly.assertThat(last.getMessage()).contains("PIN_REGISTERED");
            });
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        void 방장이_수정하면_200과_바뀐_내용을_반환한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);
            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "예전 공지"))
                    .post(PIN_PATH, post.getId())
                    .then().statusCode(201);

            Map<String, Object> response = authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "바뀐 공지"))
                    .put(PIN_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().jsonPath().getMap("$");

            assertThat(response.get("content")).isEqualTo("바뀐 공지");
        }

        @Test
        void 등록된_것이_없으면_수정할_수_없다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);

            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "바뀐 공지"))
                    .put(PIN_PATH, post.getId())
                    .then()
                    .statusCode(CommunityChatException.CHAT_PIN_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 방장이_아니면_수정할_수_없다() {
            User writer = givenUser("작성자");
            User stranger = givenUser("남");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, stranger);
            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "예전 공지"))
                    .post(PIN_PATH, post.getId())
                    .then().statusCode(201);

            authenticated(givenAccessToken(stranger))
                    .body(Map.of("content", "바뀐 공지"))
                    .put(PIN_PATH, post.getId())
                    .then()
                    .statusCode(CommunityChatException.FORBIDDEN_NOT_CHAT_PIN_HOST.getHttpStatus().value());
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        void 방장이_삭제하면_204를_반환하고_조회하면_빈_응답이_온다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);
            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "정문에서 만나요"))
                    .post(PIN_PATH, post.getId())
                    .then().statusCode(201);

            authenticated(givenAccessToken(writer))
                    .delete(PIN_PATH, post.getId())
                    .then().statusCode(204);

            Map<String, Object> response = authenticated(givenAccessToken(writer))
                    .get(PIN_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().jsonPath().getMap("$");
            assertThat(response.get("content")).isNull();
        }

        @Test
        void 등록된_것이_없으면_삭제할_수_없다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);

            authenticated(givenAccessToken(writer))
                    .delete(PIN_PATH, post.getId())
                    .then()
                    .statusCode(CommunityChatException.CHAT_PIN_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 방장이_아니면_삭제할_수_없다() {
            User writer = givenUser("작성자");
            User stranger = givenUser("남");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, stranger);
            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "정문에서 만나요"))
                    .post(PIN_PATH, post.getId())
                    .then().statusCode(201);

            authenticated(givenAccessToken(stranger))
                    .delete(PIN_PATH, post.getId())
                    .then()
                    .statusCode(CommunityChatException.FORBIDDEN_NOT_CHAT_PIN_HOST.getHttpStatus().value());
        }
    }

    @Nested
    @DisplayName("조회")
    class Get {

        @Test
        void 방_멤버는_고정_채팅을_조회할_수_있다() {
            User writer = givenUser("작성자");
            User member = givenUser("참여자");
            CommunityPost post = givenChatRoom(writer);
            givenMember(post, member);
            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "정문에서 만나요"))
                    .post(PIN_PATH, post.getId())
                    .then().statusCode(201);

            Map<String, Object> response = authenticated(givenAccessToken(member))
                    .get(PIN_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().jsonPath().getMap("$");

            assertThat(response.get("content")).isEqualTo("정문에서 만나요");
        }

        @Test
        void 등록된_것이_없으면_content가_null인_응답을_반환한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenChatRoom(writer);

            Map<String, Object> response = authenticated(givenAccessToken(writer))
                    .get(PIN_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().jsonPath().getMap("$");

            assertSoftly(softly -> {
                softly.assertThat(response.get("content")).isNull();
                softly.assertThat(response.get("postId")).isEqualTo(post.getId().intValue());
            });
        }

        @Test
        void 멤버가_아니면_조회할_수_없다() {
            User writer = givenUser("작성자");
            User stranger = givenUser("남");
            CommunityPost post = givenChatRoom(writer);

            authenticated(givenAccessToken(stranger))
                    .get(PIN_PATH, post.getId())
                    .then()
                    .statusCode(CommunityChatException.NOT_A_CHAT_MEMBER.getHttpStatus().value());
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .get(PIN_PATH, 1)
                    .then().statusCode(401);
        }
    }
}
