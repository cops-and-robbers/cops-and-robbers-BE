package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.CommunityPostFixture;
import com.team.cops_and_robbers.community.domain.CommunityComment;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.exception.CommunityCommentException;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("커뮤니티 댓글 API")
class CommunityCommentControllerTest extends ControllerTest {

    private static final String COMMENTS_PATH = "/api/community-posts/{postId}/comments";
    private static final String COMMENT_PATH = "/api/community-posts/comments/{commentId}";
    private static final String COMMENT_NOTIFICATION_PATH = COMMENT_PATH + "/notification";

    private CommunityPost givenPost(User writer) {
        return communityPostRepository.save(CommunityPostFixture.POST(writer.getId()));
    }

    private CommunityComment givenComment(CommunityPost post, User writer, String content) {
        return communityCommentRepository.save(
                CommunityComment.createComment(post.getId(), null, writer.getId(), content));
    }

    private CommunityComment givenReply(CommunityPost post, CommunityComment parent, User writer, String content) {
        return communityCommentRepository.save(
                CommunityComment.createComment(post.getId(), parent.getId(), writer.getId(), content));
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        void 오래된_순으로_조회하고_커서로_다음_페이지를_이어받는다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);
            for (int i = 1; i <= 3; i++) {
                givenComment(post, writer, "댓글" + i);
            }

            Map<String, Object> firstPage = unauthenticated()
                    .queryParam("size", 2)
                    .get(COMMENTS_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            List<Map<String, Object>> content = extractContent(firstPage);
            assertThat(content).hasSize(2);
            assertThat(content.get(0).get("content")).isEqualTo("댓글1");
            assertThat(content.get(1).get("content")).isEqualTo("댓글2");
            assertThat(firstPage.get("hasNext")).isEqualTo(true);

            Map<String, Object> secondPage = unauthenticated()
                    .queryParam("cursor", firstPage.get("nextCursor"))
                    .queryParam("size", 2)
                    .get(COMMENTS_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            List<Map<String, Object>> remaining = extractContent(secondPage);
            assertThat(remaining).hasSize(1);
            assertThat(remaining.getFirst().get("content")).isEqualTo("댓글3");
            assertThat(secondPage.get("hasNext")).isEqualTo(false);
        }

        @Test
        void 답글은_부모_댓글의_replies에_함께_담겨온다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);
            CommunityComment root = givenComment(post, writer, "루트 댓글");
            givenReply(post, root, writer, "답글");

            Map<String, Object> response = unauthenticated()
                    .get(COMMENTS_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            List<Map<String, Object>> content = extractContent(response);
            assertThat(content).hasSize(1);
            assertThat(extractReplies(content.getFirst())).hasSize(1);
            assertThat(extractReplies(content.getFirst()).getFirst().get("content")).isEqualTo("답글");
        }

        @Test
        void 답글이_남은_삭제된_댓글은_deleted_true로_내용_없이_내려온다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);
            CommunityComment root = givenComment(post, writer, "지워질 댓글");
            givenReply(post, root, writer, "답글");

            authenticated(givenAccessToken(writer))
                    .delete(COMMENT_PATH, root.getId())
                    .then().statusCode(204);

            Map<String, Object> response = unauthenticated()
                    .get(COMMENTS_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            Map<String, Object> deletedRoot = extractContent(response).getFirst();
            assertThat(deletedRoot.get("deleted")).isEqualTo(true);
            assertThat(deletedRoot.get("content")).isNull();
            assertThat(deletedRoot.get("writerNickname")).isNull();
            assertThat(deletedRoot.get("writerProfileIcon")).isNull();
        }

        @Test
        void 존재하지_않는_게시글_조회시_404를_응답한다() {
            unauthenticated()
                    .get(COMMENTS_PATH, 999)
                    .then().statusCode(CommunityPostException.POST_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 토큰_없이도_조회할_수_있다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);
            givenComment(post, writer, "댓글");

            unauthenticated()
                    .get(COMMENTS_PATH, post.getId())
                    .then().statusCode(200);
        }

        @Test
        void size가_허용_범위를_벗어나면_400을_응답한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);

            unauthenticated()
                    .queryParam("size", 51)
                    .get(COMMENTS_PATH, post.getId())
                    .then().statusCode(400);
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> extractContent(Map<String, Object> response) {
            return (List<Map<String, Object>>) response.get("content");
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> extractReplies(Map<String, Object> comment) {
            return (List<Map<String, Object>>) comment.get("replies");
        }
    }

    @Nested
    @DisplayName("댓글 작성")
    class CreateComment {

        @Test
        void 사용자가_댓글_작성에_성공하면_201을_응답한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);

            Map<String, Object> response = authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "몇 시에 만나나요?"))
                    .post(COMMENTS_PATH, post.getId())
                    .then().statusCode(201)
                    .extract().as(new TypeRef<>() {});

            assertThat(response.get("content")).isEqualTo("몇 시에 만나나요?");
            assertThat(response.get("writerNickname")).isEqualTo("작성자");
            assertThat(response.get("writerProfileIcon")).isEqualTo(writer.getProfileIcon());
        }

        @Test
        void parentId를_보내면_답글로_등록된다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);
            CommunityComment root = givenComment(post, writer, "루트 댓글");

            Map<String, Object> response = authenticated(givenAccessToken(writer))
                    .body(Map.of("parentId", root.getId(), "content", "답글입니다"))
                    .post(COMMENTS_PATH, post.getId())
                    .then().statusCode(201)
                    .extract().as(new TypeRef<>() {});

            assertThat(response.get("parentId")).isEqualTo(root.getId().intValue());
        }

        @Test
        void 답글에는_답글을_달_수_없다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);
            CommunityComment root = givenComment(post, writer, "루트 댓글");
            CommunityComment reply = givenReply(post, root, writer, "답글");

            authenticated(givenAccessToken(writer))
                    .body(Map.of("parentId", reply.getId(), "content", "대답글"))
                    .post(COMMENTS_PATH, post.getId())
                    .then()
                    .statusCode(CommunityCommentException.INVALID_COMMENT_DEPTH.getHttpStatus().value());
        }

        @Test
        void 존재하지_않는_게시글에는_작성할_수_없다() {
            User writer = givenUser("작성자");

            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", "내용"))
                    .post(COMMENTS_PATH, 999)
                    .then()
                    .statusCode(CommunityPostException.POST_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 내용이_비어있으면_400을_응답한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);

            authenticated(givenAccessToken(writer))
                    .body(Map.of("content", ""))
                    .post(COMMENTS_PATH, post.getId())
                    .then().statusCode(400);
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);

            unauthenticated()
                    .body(Map.of("content", "내용"))
                    .post(COMMENTS_PATH, post.getId())
                    .then().statusCode(401);
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        void 작성자가_삭제에_성공하면_204를_응답한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);
            CommunityComment comment = givenComment(post, writer, "댓글");

            authenticated(givenAccessToken(writer))
                    .delete(COMMENT_PATH, comment.getId())
                    .then().statusCode(204);

            assertThat(communityCommentRepository.findById(comment.getId())).isEmpty();
        }

        @Test
        void 작성자가_아니면_403을_응답한다() {
            User writer = givenUser("작성자");
            User other = givenUser("다른유저");
            CommunityPost post = givenPost(writer);
            CommunityComment comment = givenComment(post, writer, "댓글");

            authenticated(givenAccessToken(other))
                    .delete(COMMENT_PATH, comment.getId())
                    .then()
                    .statusCode(CommunityCommentException.FORBIDDEN_NOT_COMMENT_AUTHOR.getHttpStatus().value());
        }

        @Test
        void 존재하지_않는_댓글_삭제시_404를_응답한다() {
            User writer = givenUser("작성자");

            authenticated(givenAccessToken(writer))
                    .delete(COMMENT_PATH, 999)
                    .then()
                    .statusCode(CommunityCommentException.COMMENT_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .delete(COMMENT_PATH, 1)
                    .then().statusCode(401);
        }
    }

    @Nested
    @DisplayName("댓글별 답글 알림 설정")
    class UpdateCommentNotification {

        @Test
        void 기본값은_알림_받음이다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);
            CommunityComment comment = givenComment(post, writer, "댓글");

            assertThat(communityCommentRepository.getByCommentId(comment.getId()).isReplyNotificationsEnabled()).isTrue();
        }

        @Test
        void 끄면_저장되고_목록에도_반영된다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);
            CommunityComment comment = givenComment(post, writer, "댓글");

            authenticated(givenAccessToken(writer))
                    .body(Map.of("replyNotificationsEnabled", false))
                    .put(COMMENT_NOTIFICATION_PATH, comment.getId())
                    .then().statusCode(204);

            Map<String, Object> response = authenticated(givenAccessToken(writer))
                    .get(COMMENTS_PATH, post.getId())
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});
            List<Map<String, Object>> content = extractContent(response);
            assertThat(content.getFirst().get("replyNotificationsEnabled")).isEqualTo(false);
        }

        @Test
        void 남의_댓글은_설정할_수_없다() {
            User writer = givenUser("작성자");
            User other = givenUser("다른유저");
            CommunityPost post = givenPost(writer);
            CommunityComment comment = givenComment(post, writer, "댓글");

            authenticated(givenAccessToken(other))
                    .body(Map.of("replyNotificationsEnabled", false))
                    .put(COMMENT_NOTIFICATION_PATH, comment.getId())
                    .then()
                    .statusCode(CommunityCommentException.FORBIDDEN_NOT_COMMENT_AUTHOR.getHttpStatus().value());
        }

        @Test
        void 존재하지_않는_댓글이면_404를_응답한다() {
            User writer = givenUser("작성자");

            authenticated(givenAccessToken(writer))
                    .body(Map.of("replyNotificationsEnabled", false))
                    .put(COMMENT_NOTIFICATION_PATH, 999)
                    .then()
                    .statusCode(CommunityCommentException.COMMENT_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 수신_여부가_빠지면_400을_응답한다() {
            User writer = givenUser("작성자");
            CommunityPost post = givenPost(writer);
            CommunityComment comment = givenComment(post, writer, "댓글");

            authenticated(givenAccessToken(writer))
                    .body(Map.of())
                    .put(COMMENT_NOTIFICATION_PATH, comment.getId())
                    .then().statusCode(400);
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .body(Map.of("replyNotificationsEnabled", false))
                    .put(COMMENT_NOTIFICATION_PATH, 1)
                    .then().statusCode(401);
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> extractContent(Map<String, Object> response) {
            return (List<Map<String, Object>>) response.get("content");
        }
    }
}
