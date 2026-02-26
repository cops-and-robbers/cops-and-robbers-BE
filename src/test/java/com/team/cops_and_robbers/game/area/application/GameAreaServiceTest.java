package com.team.cops_and_robbers.game.area.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.application.dto.command.GameAreaCommand;
import com.team.cops_and_robbers.game.area.application.dto.result.GameAreaResult;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static com.team.cops_and_robbers.common.fixture.GameAreaFixture.GAME_AREA;
import static com.team.cops_and_robbers.common.fixture.GameFixture.FINISHED_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.HOST_PARTICIPANT;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class GameAreaServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameAreaService gameAreaService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_GAME_ID = 1L;

    private User user;
    private Game waitingGame;
    private Game inProgressGame;
    private Game finishedGame;
    private GameParticipant participant;
    private GameArea gameArea;

    @BeforeEach
    void setUp() {
        user = USER();
        waitingGame = WAITING_GAME();
        inProgressGame = IN_PROGRESS_GAME();
        finishedGame = FINISHED_GAME();
        setId(user, TEST_USER_ID);
        setId(waitingGame, TEST_GAME_ID);
        setId(inProgressGame, TEST_GAME_ID);
        setId(finishedGame, TEST_GAME_ID);
        participant = HOST_PARTICIPANT(waitingGame, user);
        gameArea = GAME_AREA(waitingGame);
    }

    @Nested
    @DisplayName("맵 정보 조회")
    class GetGameArea {

        @Test
        void WAITING_상태_게임의_맵_정보_조회에_성공한다() {
            // given
            GameAreaCommand command = GameAreaCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID)).willReturn(participant);
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(gameArea);

            // when
            GameAreaResult result = gameAreaService.getGameArea(command);

            // then
            assertThat(result.playgroundCenter().latitude()).isEqualTo(gameArea.getPlaygroundCenter().getY());
            assertThat(result.playgroundCenter().longitude()).isEqualTo(gameArea.getPlaygroundCenter().getX());
            assertThat(result.playgroundRadiusInMeters()).isEqualTo(gameArea.getPlaygroundRadiusInMeters());
            assertThat(result.jailCenter().latitude()).isEqualTo(gameArea.getJailCenter().getY());
            assertThat(result.jailCenter().longitude()).isEqualTo(gameArea.getJailCenter().getX());
            assertThat(result.jailRadiusInMeters()).isEqualTo(gameArea.getJailRadiusInMeters());
        }

        @Test
        void IN_PROGRESS_상태_게임의_맵_정보_조회에_성공한다() {
            // given
            GameAreaCommand command = GameAreaCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(inProgressGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID)).willReturn(participant);
            given(gameAreaRepository.getByGameId(TEST_GAME_ID)).willReturn(gameArea);

            // when
            GameAreaResult result = gameAreaService.getGameArea(command);

            // then
            assertThat(result.playgroundRadiusInMeters()).isEqualTo(gameArea.getPlaygroundRadiusInMeters());
            assertThat(result.jailRadiusInMeters()).isEqualTo(gameArea.getJailRadiusInMeters());
        }

        @Test
        void 게임이_비활성_상태이면_예외가_발생한다() {
            // given
            GameAreaCommand command = GameAreaCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(finishedGame);

            // when & then
            assertThatThrownBy(() -> gameAreaService.getGameArea(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameException.GAME_NOT_ACTIVE.getDetail());
        }

        @Test
        void 해당_게임의_참가자가_아니면_예외가_발생한다() {
            // given
            GameAreaCommand command = GameAreaCommand.of(TEST_USER_ID, TEST_GAME_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(waitingGame);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID))
                    .willThrow(new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> gameAreaService.getGameArea(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.PARTICIPANT_NOT_FOUND.getDetail());
        }
    }
}
