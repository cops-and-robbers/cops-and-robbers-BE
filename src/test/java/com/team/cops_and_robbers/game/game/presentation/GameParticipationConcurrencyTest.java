package com.team.cops_and_robbers.game.game.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.common.fixture.GameAreaFixture;
import com.team.cops_and_robbers.game.area.domain.AreaType;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.presentation.dto.request.CoordinatesRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameAreaRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameCreateRequest;
import com.team.cops_and_robbers.game.game.presentation.dto.request.GameSettingsRequest;
import com.team.cops_and_robbers.game.participant.presentation.dto.request.GameJoinRequest;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("게임 참가 동시성")
class GameParticipationConcurrencyTest extends ControllerTest {

    private static final long RETRY_DELAY_MILLIS = 500;

    @Autowired
    private DataSource dataSource;

    @Test
    void 첫_방_생성이_지연된_사이_재시도해도_방은_하나만_생성된다() throws Exception {
        // given
        User user = givenUser();
        String token = givenAccessToken(user);

        // when
        int[] statuses = sendTwiceWhileFirstIsDelayed(user, () -> createGame(token));

        // then
        assertSoftly(softly -> {
            softly.assertThat(statuses).containsExactlyInAnyOrder(201, 409);
            softly.assertThat(gameRepository.count()).isEqualTo(1);
            softly.assertThat(gameParticipantRepository.count()).isEqualTo(1);
        });
    }

    @Test
    void 첫_참가가_지연된_사이_다른_방에_참가해도_하나만_참가된다() throws Exception {
        // given
        Game firstGame = givenWaitingGame("host1");
        Game secondGame = givenWaitingGame("host2");
        User user = givenUser("guest");
        String token = givenAccessToken(user);

        // when
        int[] statuses = sendTwiceWhileFirstIsDelayed(
                user,
                () -> joinGame(token, firstGame.getInviteCode()),
                () -> joinGame(token, secondGame.getInviteCode())
        );

        // then
        assertSoftly(softly -> {
            softly.assertThat(statuses).containsExactlyInAnyOrder(200, 409);
            softly.assertThat(gameParticipantRepository.existsActiveGameByUserId(user.getId())).isTrue();
            softly.assertThat(gameParticipantRepository.count()).isEqualTo(3);
        });
    }

    private int[] sendTwiceWhileFirstIsDelayed(User user, Supplier<Integer> request) throws Exception {
        return sendTwiceWhileFirstIsDelayed(user, request, request);
    }

    /**
     * 다른 트랜잭션이 유저 행을 잡고 있는 동안 두 요청을 차례로 보내, 첫 요청이 커밋 전일 때 재시도가 들어오는 상황을 만든다.
     */
    private int[] sendTwiceWhileFirstIsDelayed(User user, Supplier<Integer> first, Supplier<Integer> second) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            lockUserRow(connection, user.getId());

            CompletableFuture<Integer> firstResponse = CompletableFuture.supplyAsync(first);
            Thread.sleep(RETRY_DELAY_MILLIS);
            CompletableFuture<Integer> secondResponse = CompletableFuture.supplyAsync(second);
            Thread.sleep(RETRY_DELAY_MILLIS);

            connection.commit();
            return new int[]{firstResponse.join(), secondResponse.join()};
        }
    }

    private void lockUserRow(Connection connection, Long userId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("select id from users where id = ? for update")) {
            statement.setLong(1, userId);
            statement.executeQuery();
        }
    }

    private Game givenWaitingGame(String hostNickname) {
        Game game = gameRepository.save(WAITING_GAME());
        gameAreaRepository.save(GameAreaFixture.CIRCLE_GAME_AREA(game));
        givenHost(game, givenUser(hostNickname));
        return game;
    }

    private int createGame(String token) {
        return authenticated(token)
                .body(createGameCreateRequest())
                .post("/api/games")
                .statusCode();
    }

    private int joinGame(String token, String inviteCode) {
        return authenticated(token)
                .body(new GameJoinRequest(inviteCode))
                .post("/api/games/join")
                .statusCode();
    }

    private GameCreateRequest createGameCreateRequest() {
        return new GameCreateRequest(
                new GameAreaRequest(
                        AreaType.CIRCLE,
                        new GameAreaRequest.CircleAreaRequest(
                                new CoordinatesRequest(37.5665, 126.978),
                                1000,
                                new CoordinatesRequest(37.5665, 126.978),
                                100
                        ),
                        null
                ),
                new GameSettingsRequest(30, 5, 3, 10, null),
                null
        );
    }
}
