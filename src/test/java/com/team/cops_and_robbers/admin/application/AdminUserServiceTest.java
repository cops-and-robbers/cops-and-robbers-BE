package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.SortDirection;
import com.team.cops_and_robbers.admin.application.dto.command.user.AdminTermsResetCommand;
import com.team.cops_and_robbers.admin.application.dto.command.user.AdminUserListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminTermsResetPreviewResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminTermsResetResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminUserPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminUserResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.GameParticipationResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.UserDeviceResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.UserGameHistoryResult;
import com.team.cops_and_robbers.admin.exception.AdminException;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.common.fixture.GameResultFixture;
import com.team.cops_and_robbers.common.fixture.GameResultParticipantFixture;
import com.team.cops_and_robbers.common.fixture.UserDeviceFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.history.domain.GameEndReason;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import com.team.cops_and_robbers.history.repository.RadioUsageProjection;
import com.team.cops_and_robbers.user.domain.TermsType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;

class AdminUserServiceTest extends ServiceUnitTest {

    @InjectMocks
    private AdminUserService adminUserService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = UserFixture.USER("user1");
        setId(user1, 1L);
        ReflectionTestUtils.setField(user1, "createdAt", LocalDateTime.now());

        user2 = UserFixture.KAKAO_USER();
        setId(user2, 2L);
        ReflectionTestUtils.setField(user2, "createdAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("약관 동의 초기화")
    class ResetTermsAgreement {

        @Test
        void 실행하지_않고_영향받을_사용자_수를_미리_조회한다() {
            // given
            List<TermsType> types = List.of(TermsType.LOCATION_TERMS);
            given(userRepository.countTermsResetTargets(types)).willReturn(12L);

            // when
            AdminTermsResetPreviewResult preview =
                    adminUserService.getTermsResetPreview(AdminTermsResetCommand.of(types));

            // then
            assertThat(preview.affectedUsers()).isEqualTo(12);
        }

        @Test
        void 선택한_약관의_동의를_해제하고_영향받은_인원을_반환한다() {
            // given
            List<TermsType> types = List.of(TermsType.TERMS_OF_SERVICE, TermsType.MARKETING);
            given(userRepository.resetTermsAgreement(types)).willReturn(7L);

            // when
            AdminTermsResetResult result =
                    adminUserService.resetTermsAgreement(AdminTermsResetCommand.of(types));

            // then
            assertThat(result.affectedUsers()).isEqualTo(7);
        }

        @Test
        void 초기화할_약관을_지정하지_않으면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> AdminTermsResetCommand.of(List.of()))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(AdminException.EMPTY_TERMS_RESET_TARGET.getDetail());
        }
    }

    @Nested
    @DisplayName("유저 목록 조회")
    class GetUserList {

        @Test
        void 필터_없이_유저_목록을_조회하면_전체_유저_페이지를_반환한다() {
            // given
            AdminUserListCommand command = new AdminUserListCommand(
                    0, 10, null, null, null, null, SortDirection.DESC);
            Page<User> userPage = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 10), 2);
            given(userRepository.findAllForAdmin(isNull(), isNull(), isNull(), isNull(), any()))
                    .willReturn(userPage);

            // when
            AdminUserPageResult result = adminUserService.getUserList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(2);
                softly.assertThat(result.content()).hasSize(2);
                softly.assertThat(result.content().get(0).nickname()).isEqualTo("user1");
            });
        }

        @Test
        void 닉네임_필터로_유저_목록을_조회하면_해당_닉네임_유저만_반환한다() {
            // given
            AdminUserListCommand command = new AdminUserListCommand(
                    0, 10, "user1", null, null, null, SortDirection.DESC);
            Page<User> userPage = new PageImpl<>(List.of(user1), PageRequest.of(0, 10), 1);
            given(userRepository.findAllForAdmin(eq("user1"), isNull(), isNull(), isNull(), any()))
                    .willReturn(userPage);

            // when
            AdminUserPageResult result = adminUserService.getUserList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isEqualTo(1);
                softly.assertThat(result.content().get(0).nickname()).isEqualTo("user1");
            });
        }

        @Test
        void fromDate_toDate_필터로_유저_목록을_조회하면_해당_기간의_유저만_반환한다() {
            // given
            AdminUserListCommand command = new AdminUserListCommand(
                    0, 10, null, null,
                    LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                    LocalDateTime.of(2024, 12, 31, 23, 59, 59),
                    SortDirection.ASC);
            Page<User> userPage = new PageImpl<>(List.of(user1), PageRequest.of(0, 10), 1);
            given(userRepository.findAllForAdmin(isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                    .willReturn(userPage);

            // when
            AdminUserPageResult result = adminUserService.getUserList(command);

            // then
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        void 결과가_없으면_빈_페이지를_반환한다() {
            // given
            AdminUserListCommand command = new AdminUserListCommand(
                    0, 10, "존재하지않는닉네임", null, null, null, SortDirection.DESC);
            Page<User> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(userRepository.findAllForAdmin(anyString(), isNull(), isNull(), isNull(), any()))
                    .willReturn(emptyPage);

            // when
            AdminUserPageResult result = adminUserService.getUserList(command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.totalElements()).isZero();
                softly.assertThat(result.content()).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("유저 단건 조회")
    class GetUser {

        @Test
        void 유저_아이디로_유저를_조회하면_해당_유저_정보를_반환한다() {
            // given
            given(userRepository.getByUserId(1L)).willReturn(user1);

            // when
            AdminUserResult result = adminUserService.getUser(1L);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.id()).isEqualTo(1L);
                softly.assertThat(result.nickname()).isEqualTo("user1");
                softly.assertThat(result.socialType()).isEqualTo(user1.getSocialType());
            });
        }
    }

    @Nested
    @DisplayName("유저별 디바이스 배치 조회")
    class GetDevicesByUsers {

        @Test
        void 디바이스가_있는_유저는_디바이스_정보가_포함된다() {
            // given
            UserDevice device = UserDeviceFixture.IOS_DEVICE(user1);
            ReflectionTestUtils.setField(device, "createdAt", LocalDateTime.now());
            AdminUserResult adminUser1 = AdminUserResult.from(user1);
            AdminUserResult adminUser2 = AdminUserResult.from(user2);
            List<AdminUserResult> users = List.of(adminUser1, adminUser2);

            given(userDeviceRepository.findByUser_IdIn(List.of(1L, 2L)))
                    .willReturn(List.of(device));

            // when
            Map<AdminUserResult, UserDeviceResult> result = adminUserService.getDevicesByUsers(users);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result).containsKey(adminUser1);
                softly.assertThat(result).doesNotContainKey(adminUser2);
            });
        }

        @Test
        void 디바이스가_없는_유저는_결과_맵에_포함되지_않는다() {
            // given
            AdminUserResult adminUser = AdminUserResult.from(user1);
            given(userDeviceRepository.findByUser_IdIn(anyList())).willReturn(List.of());

            // when
            Map<AdminUserResult, UserDeviceResult> result = adminUserService.getDevicesByUsers(List.of(adminUser));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void 여러_유저의_디바이스를_한번에_조회한다() {
            // given
            UserDevice device1 = UserDeviceFixture.IOS_DEVICE(user1);
            ReflectionTestUtils.setField(device1, "createdAt", LocalDateTime.now());
            UserDevice device2 = UserDeviceFixture.ANDROID_DEVICE(user2);
            ReflectionTestUtils.setField(device2, "createdAt", LocalDateTime.now());
            AdminUserResult adminUser1 = AdminUserResult.from(user1);
            AdminUserResult adminUser2 = AdminUserResult.from(user2);
            List<AdminUserResult> users = List.of(adminUser1, adminUser2);

            given(userDeviceRepository.findByUser_IdIn(List.of(1L, 2L)))
                    .willReturn(List.of(device1, device2));

            // when
            Map<AdminUserResult, UserDeviceResult> result = adminUserService.getDevicesByUsers(users);

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("유저별 게임 참여 이력 배치 조회")
    class GetGameParticipationsByUsers {

        @Test
        void 게임_참여_이력이_있는_유저의_참여_정보를_반환한다() {
            // given
            Game game = GameFixture.FINISHED_GAME();
            setId(game, 10L);
            ReflectionTestUtils.setField(game, "createdAt", LocalDateTime.now());

            GameParticipant participant = GameParticipantFixture.HOST_PARTICIPANT(game, user1);
            setId(participant, 100L);
            ReflectionTestUtils.setField(participant, "createdAt", LocalDateTime.now());

            AdminUserResult adminUser1 = AdminUserResult.from(user1);
            AdminUserResult adminUser2 = AdminUserResult.from(user2);
            List<AdminUserResult> users = List.of(adminUser1, adminUser2);

            given(gameParticipantRepository.findByUserIdInWithGame(List.of(1L, 2L)))
                    .willReturn(List.of(participant));

            // when
            Map<AdminUserResult, List<GameParticipationResult>> result =
                    adminUserService.getGameParticipationsByUsers(users);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(2);
                softly.assertThat(result.get(adminUser1)).hasSize(1);
                softly.assertThat(result.get(adminUser2)).isEmpty();
            });
        }

        @Test
        void 게임_참여_이력이_없으면_빈_리스트를_반환한다() {
            // given
            AdminUserResult adminUser = AdminUserResult.from(user1);
            given(gameParticipantRepository.findByUserIdInWithGame(anyList())).willReturn(List.of());

            // when
            Map<AdminUserResult, List<GameParticipationResult>> result =
                    adminUserService.getGameParticipationsByUsers(List.of(adminUser));

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.get(adminUser)).isEmpty();
            });
        }

        @Test
        void 한_유저가_여러_게임에_참여한_경우_모두_반환한다() {
            // given
            Game game1 = GameFixture.FINISHED_GAME();
            setId(game1, 10L);
            ReflectionTestUtils.setField(game1, "createdAt", LocalDateTime.now());

            Game game2 = GameFixture.FINISHED_GAME();
            setId(game2, 11L);
            ReflectionTestUtils.setField(game2, "createdAt", LocalDateTime.now());

            GameParticipant participant1 = GameParticipantFixture.GUEST_PARTICIPANT(game1, user1);
            setId(participant1, 100L);
            ReflectionTestUtils.setField(participant1, "createdAt", LocalDateTime.now());
            GameParticipant participant2 = GameParticipantFixture.GUEST_PARTICIPANT(game2, user1);
            setId(participant2, 101L);
            ReflectionTestUtils.setField(participant2, "createdAt", LocalDateTime.now());

            AdminUserResult adminUser = AdminUserResult.from(user1);
            given(gameParticipantRepository.findByUserIdInWithGame(List.of(1L)))
                    .willReturn(List.of(participant1, participant2));

            // when
            Map<AdminUserResult, List<GameParticipationResult>> result =
                    adminUserService.getGameParticipationsByUsers(List.of(adminUser));

            // then
            assertThat(result.get(adminUser)).hasSize(2);
        }
    }

    @Nested
    @DisplayName("유저별 무전 사용 시간 배치 조회")
    class GetRadioSecondsByUsers {

        @Test
        void 무전_기록이_있는_유저는_합계를_없는_유저는_0을_반환한다() {
            // given
            AdminUserResult adminUser1 = AdminUserResult.from(user1);
            AdminUserResult adminUser2 = AdminUserResult.from(user2);
            given(gameResultParticipantRepository.sumRadioSecondsGroupByUserIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(radioUsage(1L, 150L)));

            // when
            Map<AdminUserResult, Long> result = adminUserService.getRadioSecondsByUsers(List.of(adminUser1, adminUser2));

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.get(adminUser1)).isEqualTo(150L);
                softly.assertThat(result.get(adminUser2)).isZero();
            });
        }
    }

    @Nested
    @DisplayName("유저별 끝난 게임 이력 조회")
    class GetGameHistories {

        @Test
        void 끝난_게임_기록을_이력으로_변환해_반환한다() {
            // given
            GameResult gameResult = GameResultFixture.POLICE_WIN_RESULT(10L);
            setId(gameResult, 1L);
            ReflectionTestUtils.setField(gameResult, "hostUserId", 1L);
            ReflectionTestUtils.setField(gameResult, "eventGame", false);

            GameResultParticipant participant =
                    GameResultParticipantFixture.POLICE_PARTICIPANT(gameResult, 1L, "user1");
            participant.incrementArrestCount();
            participant.markMvp();

            given(gameResultParticipantRepository.findLatestCompletedByUserId(eq(1L), any()))
                    .willReturn(List.of(participant));

            // when
            List<UserGameHistoryResult> result = adminUserService.getGameHistories(1L, 20);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.get(0).gameResultId()).isEqualTo(1L);
                softly.assertThat(result.get(0).gameId()).isEqualTo(10L);
                softly.assertThat(result.get(0).endedAt()).isNotNull();
                softly.assertThat(result.get(0).team()).isEqualTo(Team.POLICE);
                softly.assertThat(result.get(0).winnerTeam()).isEqualTo(Team.POLICE);
                softly.assertThat(result.get(0).endReason()).isEqualTo(GameEndReason.ALL_ARRESTED);
                softly.assertThat(result.get(0).durationSeconds()).isEqualTo(300);
                softly.assertThat(result.get(0).arrestCount()).isEqualTo(1);
                softly.assertThat(result.get(0).arrestedCount()).isZero();
                softly.assertThat(result.get(0).isMvp()).isTrue();
                softly.assertThat(result.get(0).isHost()).isTrue();
                softly.assertThat(result.get(0).isEventGame()).isFalse();
                softly.assertThat(result.get(0).leftAt()).isNull();
            });
        }

        @Test
        void 방장_정보가_없거나_다른_유저면_방장이_아니다() {
            // given
            GameResult noHostResult = GameResultFixture.POLICE_WIN_RESULT(10L);
            setId(noHostResult, 1L);
            GameResult otherHostResult = GameResultFixture.ROBBER_WIN_RESULT(11L);
            setId(otherHostResult, 2L);
            ReflectionTestUtils.setField(otherHostResult, "hostUserId", 2L);

            given(gameResultParticipantRepository.findLatestCompletedByUserId(eq(1L), any()))
                    .willReturn(List.of(
                            GameResultParticipantFixture.POLICE_PARTICIPANT(noHostResult, 1L, "user1"),
                            GameResultParticipantFixture.ROBBER_PARTICIPANT(otherHostResult, 1L, "user1")
                    ));

            // when
            List<UserGameHistoryResult> result = adminUserService.getGameHistories(1L, 20);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.get(0).isHost()).isFalse();
                softly.assertThat(result.get(1).isHost()).isFalse();
            });
        }

        @Test
        void 이벤트_여부가_기록되기_전_게임은_이벤트_게임이_아니다() {
            // given
            GameResult legacyResult = GameResultFixture.LEGACY_RESULT_WITHOUT_AREA(10L);
            setId(legacyResult, 1L);

            given(gameResultParticipantRepository.findLatestCompletedByUserId(eq(1L), any()))
                    .willReturn(List.of(GameResultParticipantFixture.POLICE_PARTICIPANT(legacyResult, 1L, "user1")));

            // when
            List<UserGameHistoryResult> result = adminUserService.getGameHistories(1L, 20);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.get(0).isEventGame()).isFalse();
                softly.assertThat(result.get(0).endedAt()).isNull();
            });
        }

        @Test
        void 게임_중_나간_기록은_퇴장_시각이_있다() {
            // given
            GameResult gameResult = GameResultFixture.POLICE_WIN_RESULT(10L);
            setId(gameResult, 1L);
            GameResultParticipant left = GameResultParticipantFixture.ROBBER_PARTICIPANT(gameResult, 1L, "user1");
            left.markLeft(LocalDateTime.now());

            given(gameResultParticipantRepository.findLatestCompletedByUserId(eq(1L), any()))
                    .willReturn(List.of(left));

            // when
            List<UserGameHistoryResult> result = adminUserService.getGameHistories(1L, 20);

            // then
            assertThat(result.get(0).leftAt()).isNotNull();
        }

        @Test
        void 끝난_게임_기록이_없으면_빈_리스트를_반환한다() {
            // given
            given(gameResultParticipantRepository.findLatestCompletedByUserId(eq(1L), any()))
                    .willReturn(List.of());

            // when
            List<UserGameHistoryResult> result = adminUserService.getGameHistories(1L, 20);

            // then
            assertThat(result).isEmpty();
        }
    }

    private RadioUsageProjection radioUsage(Long userId, Long radioSeconds) {
        return new RadioUsageProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public Long getRadioSeconds() {
                return radioSeconds;
            }
        };
    }
}
