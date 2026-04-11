package com.team.cops_and_robbers.play.location.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.play.location.application.dto.command.LocationUpdateCommand;
import com.team.cops_and_robbers.play.location.application.dto.command.RobberLocationsCommand;
import com.team.cops_and_robbers.play.location.application.dto.result.RobberLocationResult;
import com.team.cops_and_robbers.play.location.repository.RobberLocationRepository;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.GameFixture.IN_PROGRESS_GAME;
import static com.team.cops_and_robbers.common.fixture.GameFixture.WAITING_GAME;
import static com.team.cops_and_robbers.common.fixture.GameParticipantFixture.ALIVE_ROBBER;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class RobberLocationServiceTest extends ServiceUnitTest {

    @InjectMocks
    private RobberLocationService robberLocationService;

    @Mock
    private RobberLocationRepository robberLocationRepository;

    private static final Long TEST_GAME_ID = 1L;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_PARTICIPANT_ID = 1L;

    private User user;
    private Game game;
    private GameParticipant robberParticipant;

    @BeforeEach
    void setUp() {
        user = USER();
        game = IN_PROGRESS_GAME();
        setId(user, TEST_USER_ID);
        setId(game, TEST_GAME_ID);
        robberParticipant = ALIVE_ROBBER(game, user);
        setId(robberParticipant, TEST_PARTICIPANT_ID);
    }

    @Nested
    @DisplayName("도둑 위치 조회")
    class GetRobberLocations {

        @Test
        void 위치를_전송한_도둑의_위치가_반환된다() {
            // given
            given(gameParticipantRepository.findByIdWithUser(TEST_PARTICIPANT_ID))
                    .willReturn(Optional.of(robberParticipant));
            robberLocationService.updateLocation(new LocationUpdateCommand(TEST_GAME_ID, TEST_PARTICIPANT_ID, 37.5665, 126.9780));

            RobberLocationsCommand command = RobberLocationsCommand.of(TEST_GAME_ID, TEST_USER_ID);
            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(robberLocationRepository.findAllByGameId(TEST_GAME_ID))
                    .willReturn(List.of(SystemEventData.RobberLocation.of(TEST_PARTICIPANT_ID, user.getNickname(), 37.5665, 126.9780)));
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID)).willReturn(robberParticipant);

            // when
            List<RobberLocationResult> results = robberLocationService.getRobberLocations(command);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).participantId()).isEqualTo(TEST_PARTICIPANT_ID);
            assertThat(results.get(0).nickname()).isEqualTo(user.getNickname());
            assertThat(results.get(0).latitude()).isEqualTo(37.5665);
            assertThat(results.get(0).longitude()).isEqualTo(126.9780);
        }

        @Test
        void 위치를_전송하지_않은_경우_빈_목록이_반환된다() {
            // given
            RobberLocationsCommand command = RobberLocationsCommand.of(TEST_GAME_ID, TEST_USER_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID)).willReturn(robberParticipant);

            // when
            List<RobberLocationResult> results = robberLocationService.getRobberLocations(command);

            // then
            assertThat(results).isEmpty();
        }

        @Test
        void 게임이_진행_중이_아니면_예외가_발생한다() {
            // given
            Game waitingGame = WAITING_GAME();
            setId(waitingGame, TEST_GAME_ID);
            RobberLocationsCommand command = RobberLocationsCommand.of(TEST_GAME_ID, TEST_USER_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(waitingGame);

            // when & then
            assertThatThrownBy(() -> robberLocationService.getRobberLocations(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameException.GAME_NOT_IN_PROGRESS.getDetail());
        }

        @Test
        void 게임이_존재하지_않으면_예외가_발생한다() {
            // given
            RobberLocationsCommand command = RobberLocationsCommand.of(TEST_GAME_ID, TEST_USER_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID))
                    .willThrow(new ApplicationException(GameException.GAME_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> robberLocationService.getRobberLocations(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameException.GAME_NOT_FOUND.getDetail());
        }

        @Test
        void 해당_게임의_참가자가_아니면_예외가_발생한다() {
            // given
            RobberLocationsCommand command = RobberLocationsCommand.of(TEST_GAME_ID, TEST_USER_ID);

            given(gameRepository.getByGameId(TEST_GAME_ID)).willReturn(game);
            given(gameParticipantRepository.getByGameIdAndUserId(TEST_GAME_ID, TEST_USER_ID))
                    .willThrow(new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> robberLocationService.getRobberLocations(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(GameParticipantException.PARTICIPANT_NOT_FOUND.getDetail());
        }
    }
}
