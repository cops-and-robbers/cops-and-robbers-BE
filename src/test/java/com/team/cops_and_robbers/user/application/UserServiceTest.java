package com.team.cops_and_robbers.user.application;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.user.application.dto.command.NicknameUpdateCommand;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.exception.UserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;

import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class UserServiceTest extends ServiceUnitTest {

    @InjectMocks
    private UserService userService;

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
            then(userRepository).should().delete(user);
            then(firebaseAuth).should().deleteUser(user.getSocialId());
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
            then(userRepository).should().delete(user);
        }
    }

}
