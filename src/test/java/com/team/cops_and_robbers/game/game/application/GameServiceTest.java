package com.team.cops_and_robbers.game.game.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.domain.GameAreaDomainService;
import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
import com.team.cops_and_robbers.game.game.application.dto.result.GameCreateResult;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class GameServiceTest extends ServiceUnitTest {

    @InjectMocks
    private GameService gameService;

    @Mock
    private GameAreaDomainService gameAreaDomainService;

    private static final Long TEST_HOST_ID = 1L;
    private static final Long TEST_GAME_ID = 1L;

    private User host;
    private Game game;

    @BeforeEach
    void setUp() {
        host = USER();
        game = WAITING_GAME();
        setId(host, TEST_HOST_ID);
        setId(game, TEST_GAME_ID);
    }

    @Nested
    @DisplayName("게임 방 생성")
    class CreateGame {

        @Test
        void 게임_생성에_성공하면_결과를_반환한다() {
            // given
            GameCreateCommand command = createGameCommand(host.getId());

            given(userRepository.getByUserId(host.getId())).willReturn(host);
            given(gameParticipantRepository.existsActiveGameByUserId(host.getId())).willReturn(false);
            given(gameRepository.save(any(Game.class))).willReturn(game);
            given(gameRepository.existsByInviteCode(anyString())).willReturn(false);

            // when
            GameCreateResult result = gameService.createGame(command);

            // then
            assertThat(result.inviteCode()).isEqualTo("ABC123");

            then(gameAreaDomainService).should().validateAreaContainment(anyDouble(), anyDouble(), anyInt(), anyDouble(), anyDouble(), anyInt());
            then(gameRepository).should().save(any(Game.class));
            then(gameAreaRepository).should().save(any(GameArea.class));
            then(gameParticipantRepository).should().save(any(GameParticipant.class));
        }

        @Test
        void 이미_참여_중인_활성_게임이_있는_유저라면_예외가_발생한다() {
            // given
            GameCreateCommand command = createGameCommand(host.getId());

            given(userRepository.getByUserId(host.getId())).willReturn(host);
            given(gameParticipantRepository.existsActiveGameByUserId(host.getId())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> gameService.createGame(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.ALREADY_PARTICIPATING.getDetail());

            then(gameRepository).should(never()).save(any(Game.class));
            then(gameAreaRepository).should(never()).save(any(GameArea.class));
            then(gameParticipantRepository).should(never()).save(any(GameParticipant.class));
        }
    }

    private GameCreateCommand createGameCommand(Long hostId) {
        return new GameCreateCommand(
                hostId,
                37.5665, 126.978, 1000, // playgroundLat, playgroundLon, playgroundRadius
                37.5665, 126.978, 100,  // jailLat, jailLon, jailRadius
                30, // roundDurationMinutes
                5,  // locationRevealIntervalMinutes
                3,  // policeWaitMinutes
                10  // maxParticipants
        );
    }
}
