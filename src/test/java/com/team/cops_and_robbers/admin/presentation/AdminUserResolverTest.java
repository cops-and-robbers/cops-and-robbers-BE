package com.team.cops_and_robbers.admin.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.common.fixture.UserDeviceFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class AdminUserResolverTest extends ControllerTest {

    private static final String GRAPHQL_URL = "/graphql";

    private String adminToken;

    @BeforeEach
    void setUpAdmin() {
        adminToken = givenAccessToken(givenAdminUser("admin"));
    }

    @Nested
    @DisplayName("인증 검사")
    class Authentication {

        @Test
        void 토큰_없이_요청하면_401을_반환한다() {
            // when
            ExtractableResponse<Response> response = executeQuery(
                    "{ adminUsers { totalElements content { id } } }");

            // then
            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        void 일반_유저_토큰으로_요청하면_403을_반환한다() {
            // given
            User user = givenUser("normalUser");
            String userToken = givenAccessToken(user);

            // when
            ExtractableResponse<Response> response = executeQuery(
                    userToken, "{ adminUsers { totalElements content { id } } }");

            // then
            assertThat(response.statusCode()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("유저 목록 조회")
    class AdminUsers {

        @Test
        void 유저_목록을_페이지로_조회한다() {
            // given
            givenUser("user1");
            givenUser("user2");

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminUsers(page: 0, size: 10) { totalElements totalPages page size content { id nickname socialType role } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminUsers.totalElements")).isGreaterThanOrEqualTo(2);
                softly.assertThat(response.jsonPath().getList("data.adminUsers.content")).isNotEmpty();
            });
        }

        @Test
        void 닉네임으로_유저를_필터링한다() {
            // given
            givenUser("uniqueNick");
            givenUser("otherUser");

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminUsers(nickname: \"uniqueNick\") { totalElements content { id nickname } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminUsers.totalElements")).isEqualTo(1);
                softly.assertThat(response.jsonPath().getString("data.adminUsers.content[0].nickname")).isEqualTo("uniqueNick");
            });
        }

        @Test
        void 결과가_없으면_빈_목록을_반환한다() {
            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminUsers(nickname: \"존재하지않는닉네임\") { totalElements content { id } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Integer) response.jsonPath().get("data.adminUsers.totalElements")).isZero();
                softly.assertThat(response.jsonPath().getList("data.adminUsers.content")).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("유저 단건 조회")
    class AdminUser {

        @Test
        void 유저_아이디로_단건_조회한다() {
            // given
            User user = givenUser("targetUser");

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminUser(id: " + user.getId() + ") { id nickname socialType role } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.adminUser.id")).isEqualTo(String.valueOf(user.getId()));
                softly.assertThat(response.jsonPath().getString("data.adminUser.nickname")).isEqualTo("targetUser");
            });
        }

        @Test
        void 디바이스_정보를_포함하여_조회한다() {
            // given
            User user = givenUser("deviceUser");
            UserDevice device = userDeviceRepository.save(UserDeviceFixture.IOS_DEVICE(user));

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminUser(id: " + user.getId() + ") { id nickname device { deviceType } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getString("data.adminUser.device.deviceType")).isEqualTo("IOS");
            });
        }

        @Test
        void 디바이스가_없는_유저는_device가_null이다() {
            // given
            User user = givenUser("noDeviceUser");

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminUser(id: " + user.getId() + ") { id nickname device { deviceType } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat((Object) response.jsonPath().get("data.adminUser.device")).isNull();
            });
        }

        @Test
        void 게임_참여_이력을_포함하여_조회한다() {
            // given
            User user = givenUser("participantUser");
            Game game = gameRepository.save(GameFixture.FINISHED_GAME());
            GameParticipant participant = gameParticipantRepository.save(
                    GameParticipantFixture.HOST_PARTICIPANT(game, user));

            // when
            ExtractableResponse<Response> response = executeQuery(
                    adminToken,
                    "{ adminUser(id: " + user.getId() + ") { id nickname participations { gameId status isHost } } }");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.jsonPath().getList("data.adminUser.participations")).hasSize(1);
                softly.assertThat(response.jsonPath().getString("data.adminUser.participations[0].gameId"))
                        .isEqualTo(String.valueOf(game.getId()));
                softly.assertThat((Boolean) response.jsonPath().get("data.adminUser.participations[0].isHost")).isTrue();
            });
        }
    }

    private ExtractableResponse<Response> executeQuery(String token, String query) {
        return authenticated(token)
                .body("{\"query\": \"" + query.replace("\"", "\\\"").replace("\n", " ") + "\"}")
                .when()
                .post(GRAPHQL_URL)
                .then()
                .extract();
    }

    private ExtractableResponse<Response> executeQuery(String query) {
        return unauthenticated()
                .body("{\"query\": \"" + query.replace("\"", "\\\"").replace("\n", " ") + "\"}")
                .when()
                .post(GRAPHQL_URL)
                .then()
                .extract();
    }
}
