package com.team.cops_and_robbers.user.application;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.fixture.CommunityPostFixture;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.domain.GameStatus;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.user.application.dto.command.AgreementCommand;
import com.team.cops_and_robbers.user.application.dto.command.GamePushAgreementCommand;
import com.team.cops_and_robbers.user.application.dto.command.NicknameUpdateCommand;
import com.team.cops_and_robbers.user.application.dto.result.GamePushAgreementResult;
import com.team.cops_and_robbers.user.application.dto.result.UserGameInfoResult;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class UserServiceTest extends ServiceUnitTest {

    @InjectMocks
    private UserService userService;

    @Spy
    private Clock clock = Clock.systemDefaultZone();

    @Mock
    protected RefreshTokenRepository refreshTokenRepository;

    @Mock
    private FirebaseAuth firebaseAuth;

    private User user;
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        user = USER();
        setId(user, TEST_USER_ID);
    }

    @Nested
    @DisplayName("약관 동의")
    class UpdateTermsAgreement {

        @Test
        void 필수_약관과_마케팅_미동의로_약관_동의에_성공한다() {
            // given
            AgreementCommand command = AgreementCommand.of(TEST_USER_ID, true, true, true, false);
            given(userRepository.getByUserId(TEST_USER_ID)).willReturn(user);

            // when
            userService.updateTermsAgreement(command);

            // then
            assertThat(user.isTermsOfServiceAgreed()).isTrue();
            assertThat(user.isPrivacyPolicyAgreed()).isTrue();
            assertThat(user.isLocationTermsAgreed()).isTrue();
            assertThat(user.getTermsAgreedAt()).isNotNull();
            assertThat(user.isAllowMarketingPush()).isFalse();
            assertThat(user.getMarketingAgreedAt()).isNull();
        }

        @Test
        void 마케팅_동의시_마케팅_동의_시각이_함께_저장된다() {
            // given
            AgreementCommand command = AgreementCommand.of(TEST_USER_ID, true, true, true, true);
            given(userRepository.getByUserId(TEST_USER_ID)).willReturn(user);

            // when
            userService.updateTermsAgreement(command);

            // then
            assertThat(user.isAllowMarketingPush()).isTrue();
            assertThat(user.getMarketingAgreedAt()).isNotNull();
        }

        @Test
        void 필수_약관에_미동의하면_예외가_발생한다() {
            // given
            AgreementCommand command = AgreementCommand.of(TEST_USER_ID, false, true, true, false);

            // when & then
            assertThatThrownBy(() -> userService.updateTermsAgreement(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(UserException.REQUIRED_TERMS_NOT_AGREED.getDetail());
        }
    }

    @Nested
    @DisplayName("참여 중인 게임 정보 조회")
    class GetUserGameInfo {

        @Test
        void 참여_중인_게임이_있으면_게임_정보를_반환한다() {
            // given
            Game game = GameFixture.WAITING_GAME();
            setId(game, 1L);
            ReflectionTestUtils.setField(game, "createdAt", LocalDateTime.now());
            GameParticipant participant = GameParticipantFixture.HOST_PARTICIPANT(game, user);
            setId(participant, 10L);

            given(gameParticipantRepository.findActiveParticipantByUserId(user.getId()))
                    .willReturn(Optional.of(participant));

            // when
            Optional<UserGameInfoResult> result = userService.getUserGameInfo(user.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().gameId()).isEqualTo(game.getId());
            assertThat(result.get().participantId()).isEqualTo(participant.getId());
            assertThat(result.get().gameStatus()).isEqualTo(GameStatus.WAITING);
            assertThat(result.get().team()).isEqualTo(Team.POLICE);
        }

        @Test
        void 참여_중인_게임이_없으면_빈_Optional을_반환한다() {
            // given
            given(gameParticipantRepository.findActiveParticipantByUserId(user.getId()))
                    .willReturn(Optional.empty());

            // when
            Optional<UserGameInfoResult> result = userService.getUserGameInfo(user.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void 만료된_WAITING_로비이면_삭제하고_빈_Optional을_반환한다() {
            // given
            Game game = GameFixture.WAITING_GAME();
            setId(game, 1L);
            ReflectionTestUtils.setField(game, "createdAt", LocalDateTime.now().minusHours(25));
            GameParticipant participant = GameParticipantFixture.HOST_PARTICIPANT(game, user);
            setId(participant, 10L);

            given(gameParticipantRepository.findActiveParticipantByUserId(user.getId()))
                    .willReturn(Optional.of(participant));

            // when
            Optional<UserGameInfoResult> result = userService.getUserGameInfo(user.getId());

            // then
            assertThat(result).isEmpty();
            then(gameParticipantRepository).should().deleteAllByGameId(game.getId());
            then(gameAreaRepository).should().deleteByGameId(game.getId());
            then(gameRepository).should().deleteByGameId(game.getId());
        }

        @Test
        void IN_PROGRESS_게임은_만료_시간이_지나도_삭제하지_않는다() {
            // given
            Game game = GameFixture.IN_PROGRESS_GAME();
            setId(game, 1L);
            ReflectionTestUtils.setField(game, "createdAt", LocalDateTime.now().minusHours(25));
            GameParticipant participant = GameParticipantFixture.HOST_PARTICIPANT(game, user);
            setId(participant, 10L);

            given(gameParticipantRepository.findActiveParticipantByUserId(user.getId()))
                    .willReturn(Optional.of(participant));

            // when
            Optional<UserGameInfoResult> result = userService.getUserGameInfo(user.getId());

            // then
            assertThat(result).isPresent();
            then(gameParticipantRepository).should(never()).deleteAllByGameId(any());
            then(gameAreaRepository).should(never()).deleteByGameId(any());
            then(gameRepository).should(never()).deleteByGameId(any());
        }
    }

    @Nested
    @DisplayName("닉네임 수정")
    class UpdateNickname {

        @Test
        void 닉네임_수정에_성공한다() {
            // given
            String newNickname = "새로운닉네임";
            NicknameUpdateCommand command = new NicknameUpdateCommand(user.getId(), newNickname);
            given(userRepository.getByUserId(user.getId())).willReturn(user);

            // when
            userService.updateNickname(command);

            // then
            assertThat(user.getNickname()).isEqualTo(newNickname);
            then(userRepository).should().flush();
        }

        @Test
        void 중복된_닉네임으로_수정하면_예외가_발생한다() {
            // given
            NicknameUpdateCommand command = new NicknameUpdateCommand(user.getId(), "중복닉네임");
            given(userRepository.getByUserId(user.getId())).willReturn(user);

            doThrow(DataIntegrityViolationException.class).when(userRepository).flush();

            // when & then
            assertThatThrownBy(() -> userService.updateNickname(command))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(UserException.DUPLICATED_NICKNAME.getDetail());
        }
    }

    @Nested
    @DisplayName("게임 푸시 알림 수신 동의 여부 조회")
    class GetGamePushAgreement {

        @Test
        void 게임_푸시_동의_여부를_반환한다() {
            // given
            given(userRepository.getByUserId(user.getId())).willReturn(user);

            // when
            GamePushAgreementResult result = userService.getGamePushAgreement(user.getId());

            // then
            assertThat(result.allowGamePush()).isEqualTo(user.isAllowGamePush());
        }
    }

    @Nested
    @DisplayName("게임 푸시 알림 수신 동의 여부 업데이트")
    class UpdateGamePushAgreement {

        @Test
        void true로_업데이트에_성공한다() {
            // given
            GamePushAgreementCommand command = GamePushAgreementCommand.of(user.getId(), true);
            given(userRepository.getByUserId(user.getId())).willReturn(user);

            // when
            userService.updateGamePushAgreement(command);

            // then
            assertThat(user.isAllowGamePush()).isTrue();
        }

        @Test
        void false로_업데이트에_성공한다() {
            // given
            GamePushAgreementCommand command = GamePushAgreementCommand.of(user.getId(), false);
            given(userRepository.getByUserId(user.getId())).willReturn(user);

            // when
            userService.updateGamePushAgreement(command);

            // then
            assertThat(user.isAllowGamePush()).isFalse();
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class DeleteAccount {

        @Test
        void 회원_탈퇴에_성공한다() throws Exception {
            // given
            given(userRepository.getByUserId(user.getId())).willReturn(user);
            given(gameParticipantRepository.existsActiveGameByUserId(user.getId())).willReturn(false);

            // when
            userService.deleteAccount(user.getId());

            // then
            then(userRepository).should().deleteUserByIdDirectly(user.getId());
            then(firebaseAuth).should().deleteUser(user.getSocialId());
            then(refreshTokenRepository).should().delete(user.getId());
        }

        @Test
        void 참여_중인_활성_게임이_있으면_탈퇴가_불가능하다() {
            // given
            given(userRepository.getByUserId(user.getId())).willReturn(user);
            given(gameParticipantRepository.existsActiveGameByUserId(user.getId())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.deleteAccount(user.getId()))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(UserException.CANNOT_WITHDRAW.getDetail());

            then(userRepository).should(never()).delete(any());
            then(refreshTokenRepository).should(never()).delete(any());
            then(firebaseAuth).shouldHaveNoInteractions();
        }

        @Test
        void 외부_인증서버에_유저가_정보가_없는_경우에도_DB삭제는_진행된다() throws Exception {
            // given
            given(userRepository.getByUserId(user.getId())).willReturn(user);
            given(gameParticipantRepository.existsActiveGameByUserId(user.getId())).willReturn(false);

            FirebaseAuthException mockException = mock(FirebaseAuthException.class);
            given(mockException.getAuthErrorCode()).willReturn(AuthErrorCode.USER_NOT_FOUND);
            doThrow(mockException).when(firebaseAuth).deleteUser(user.getSocialId());

            // when
            userService.deleteAccount(user.getId());

            // then
            then(userRepository).should().deleteUserByIdDirectly(user.getId());
            then(refreshTokenRepository).should().delete(user.getId());
        }

        @Test
        void 탈퇴하면_모집중인_게시글이_자동으로_마감된다() throws Exception {
            // given
            given(userRepository.getByUserId(user.getId())).willReturn(user);
            given(gameParticipantRepository.existsActiveGameByUserId(user.getId())).willReturn(false);

            CommunityPost recruitingPost = CommunityPostFixture.POST(user.getId());
            CommunityPost completedPost = CommunityPostFixture.COMPLETED_POST(user.getId());
            given(communityPostRepository.findAllByWriterId(user.getId()))
                    .willReturn(List.of(recruitingPost, completedPost));

            // when
            userService.deleteAccount(user.getId());

            // then
            assertThat(recruitingPost.getStatus()).isEqualTo(RecruitmentStatus.COMPLETED);
            assertThat(completedPost.getStatus()).isEqualTo(RecruitmentStatus.COMPLETED);
        }
    }

}
