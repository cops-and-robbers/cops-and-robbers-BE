package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.CommunityPostFixture;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.CommunityPostLike;
import com.team.cops_and_robbers.community.domain.CommunityPostScrap;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.exception.CommunityPostReactionException;
import com.team.cops_and_robbers.user.domain.User;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("커뮤니티 게시글 좋아요·스크랩 API")
class CommunityPostReactionControllerTest extends ControllerTest {

    private static final String LIKES_PATH = "/api/community-posts/{postId}/likes";
    private static final String SCRAPS_PATH = "/api/community-posts/{postId}/scraps";
    private static final String MY_SCRAPS_PATH = "/api/community-posts/scraps";

    private CommunityPost givenPost(User writer) {
        return communityPostRepository.save(CommunityPostFixture.POST(writer.getId()));
    }

    private void givenLiked(CommunityPost post, User user) {
        communityPostLikeRepository.save(CommunityPostLike.createLike(post.getId(), user.getId()));
    }

    private void givenScrapped(CommunityPost post, User user) {
        communityPostScrapRepository.save(CommunityPostScrap.createScrap(post.getId(), user.getId()));
    }

    @Nested
    @DisplayName("게시글 좋아요")
    class LikePost {

        @Test
        void 좋아요를_누르면_201을_응답한다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);

            authenticated(givenAccessToken(user))
                    .post(LIKES_PATH, post.getId())
                    .then().statusCode(201);

            assertThat(communityPostLikeRepository.existsByCommunityPostIdAndUserId(post.getId(), user.getId()))
                    .isTrue();
        }

        @Test
        void 이미_좋아요한_게시글에_다시_누르면_409를_응답한다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);
            givenLiked(post, user);

            authenticated(givenAccessToken(user))
                    .post(LIKES_PATH, post.getId())
                    .then()
                    .statusCode(CommunityPostReactionException.ALREADY_LIKED.getHttpStatus().value());
        }

        @Test
        void 존재하지_않는_게시글에는_좋아요를_누를_수_없다() {
            User user = givenUser("유저");

            authenticated(givenAccessToken(user))
                    .post(LIKES_PATH, 999)
                    .then()
                    .statusCode(CommunityPostException.POST_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .post(LIKES_PATH, 1)
                    .then().statusCode(401);
        }
    }

    @Nested
    @DisplayName("게시글 좋아요 취소")
    class UnlikePost {

        @Test
        void 좋아요를_취소하면_204를_응답한다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);
            givenLiked(post, user);

            authenticated(givenAccessToken(user))
                    .delete(LIKES_PATH, post.getId())
                    .then().statusCode(204);

            assertThat(communityPostLikeRepository.existsByCommunityPostIdAndUserId(post.getId(), user.getId()))
                    .isFalse();
        }

        @Test
        void 좋아요한_적_없으면_404를_응답한다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);

            authenticated(givenAccessToken(user))
                    .delete(LIKES_PATH, post.getId())
                    .then()
                    .statusCode(CommunityPostReactionException.LIKE_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .delete(LIKES_PATH, 1)
                    .then().statusCode(401);
        }
    }

    @Nested
    @DisplayName("게시글 스크랩")
    class ScrapPost {

        @Test
        void 스크랩하면_201을_응답한다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);

            authenticated(givenAccessToken(user))
                    .post(SCRAPS_PATH, post.getId())
                    .then().statusCode(201);

            assertThat(communityPostScrapRepository.existsByCommunityPostIdAndUserId(post.getId(), user.getId()))
                    .isTrue();
        }

        @Test
        void 이미_스크랩한_게시글을_다시_스크랩하면_409를_응답한다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);
            givenScrapped(post, user);

            authenticated(givenAccessToken(user))
                    .post(SCRAPS_PATH, post.getId())
                    .then()
                    .statusCode(CommunityPostReactionException.ALREADY_SCRAPPED.getHttpStatus().value());
        }

        @Test
        void 존재하지_않는_게시글은_스크랩할_수_없다() {
            User user = givenUser("유저");

            authenticated(givenAccessToken(user))
                    .post(SCRAPS_PATH, 999)
                    .then()
                    .statusCode(CommunityPostException.POST_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .post(SCRAPS_PATH, 1)
                    .then().statusCode(401);
        }
    }

    @Nested
    @DisplayName("게시글 스크랩 취소")
    class UnscrapPost {

        @Test
        void 스크랩을_취소하면_204를_응답한다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);
            givenScrapped(post, user);

            authenticated(givenAccessToken(user))
                    .delete(SCRAPS_PATH, post.getId())
                    .then().statusCode(204);

            assertThat(communityPostScrapRepository.existsByCommunityPostIdAndUserId(post.getId(), user.getId()))
                    .isFalse();
        }

        @Test
        void 스크랩한_적_없으면_404를_응답한다() {
            User user = givenUser("유저");
            CommunityPost post = givenPost(user);

            authenticated(givenAccessToken(user))
                    .delete(SCRAPS_PATH, post.getId())
                    .then()
                    .statusCode(CommunityPostReactionException.SCRAP_NOT_FOUND.getHttpStatus().value());
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .delete(SCRAPS_PATH, 1)
                    .then().statusCode(401);
        }
    }

    @Nested
    @DisplayName("내 스크랩 목록 조회")
    class GetMyScraps {

        @Test
        void 최근에_스크랩한_게시글부터_조회된다() {
            User user = givenUser("유저");
            CommunityPost older = givenPost(user);
            CommunityPost newer = givenPost(user);
            givenScrapped(older, user);
            givenScrapped(newer, user);

            Map<String, Object> response = authenticated(givenAccessToken(user))
                    .get(MY_SCRAPS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            List<Map<String, Object>> content = extractContent(response);
            assertThat(content).hasSize(2);
            assertThat(content.get(0).get("id")).isEqualTo(newer.getId().intValue());
            assertThat(content.get(1).get("id")).isEqualTo(older.getId().intValue());
        }

        @Test
        void 남이_스크랩한_게시글은_내_목록에_나오지_않는다() {
            User user = givenUser("유저");
            User other = givenUser("다른유저");
            CommunityPost post = givenPost(user);
            givenScrapped(post, other);

            Map<String, Object> response = authenticated(givenAccessToken(user))
                    .get(MY_SCRAPS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            assertThat(extractContent(response)).isEmpty();
        }

        @Test
        void 요청_크기보다_많으면_커서로_다음_페이지를_이어받는다() {
            User user = givenUser("유저");
            for (int i = 1; i <= 3; i++) {
                givenScrapped(givenPost(user), user);
            }

            Map<String, Object> firstPage = authenticated(givenAccessToken(user))
                    .queryParam("size", 2)
                    .get(MY_SCRAPS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            assertThat(extractContent(firstPage)).hasSize(2);
            assertThat(firstPage.get("hasNext")).isEqualTo(true);

            Map<String, Object> secondPage = authenticated(givenAccessToken(user))
                    .queryParam("cursor", firstPage.get("nextCursor"))
                    .queryParam("size", 2)
                    .get(MY_SCRAPS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            assertThat(extractContent(secondPage)).hasSize(1);
            assertThat(secondPage.get("hasNext")).isEqualTo(false);
        }

        @Test
        void 토큰_없이_요청하면_401을_응답한다() {
            unauthenticated()
                    .get(MY_SCRAPS_PATH)
                    .then().statusCode(401);
        }

        @Test
        void 채팅방_참여_여부가_스크랩한_게시글마다_반영된다() {
            User user = givenUser("유저");
            CommunityPost joined = givenPost(user);
            CommunityPost notJoined = givenPost(user);
            givenScrapped(joined, user);
            givenScrapped(notJoined, user);
            communityChatMemberRepository.save(CommunityChatMember.createMember(joined.getId(), user.getId()));

            Map<String, Object> response = authenticated(givenAccessToken(user))
                    .get(MY_SCRAPS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            List<Map<String, Object>> content = extractContent(response);
            Map<String, Object> joinedResult = content.stream()
                    .filter(post -> post.get("id").equals(joined.getId().intValue())).findFirst().orElseThrow();
            Map<String, Object> notJoinedResult = content.stream()
                    .filter(post -> post.get("id").equals(notJoined.getId().intValue())).findFirst().orElseThrow();
            assertThat(joinedResult.get("chatJoined")).isEqualTo(true);
            assertThat(notJoinedResult.get("chatJoined")).isEqualTo(false);
        }

        @Test
        void 스크랩_목록의_좋아요_스크랩_카운트와_내_반응이_반영된다() {
            User user = givenUser("유저");
            User other = givenUser("다른유저");
            CommunityPost post = givenPost(user);
            givenScrapped(post, user);
            givenLiked(post, other);
            givenLiked(post, user);

            Map<String, Object> response = authenticated(givenAccessToken(user))
                    .get(MY_SCRAPS_PATH)
                    .then().statusCode(200)
                    .extract().as(new TypeRef<>() {});

            Map<String, Object> content = extractContent(response).getFirst();
            assertThat(content.get("likeCount")).isEqualTo(2);
            assertThat(content.get("scrapCount")).isEqualTo(1);
            assertThat(content.get("isLikedByRequester")).isEqualTo(true);
            assertThat(content.get("isScrappedByRequester")).isEqualTo(true);
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> extractContent(Map<String, Object> response) {
            return (List<Map<String, Object>>) response.get("content");
        }
    }
}
