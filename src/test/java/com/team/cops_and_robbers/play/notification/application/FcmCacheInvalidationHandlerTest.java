package com.team.cops_and_robbers.play.notification.application;

import com.team.cops_and_robbers.auth.application.event.UserFcmTokenUpdatedEvent;
import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fixture.GameFixture;
import com.team.cops_and_robbers.common.fixture.GameParticipantFixture;
import com.team.cops_and_robbers.common.fixture.UserFixture;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.play.notification.repository.FcmTokenCacheRepository;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class FcmCacheInvalidationHandlerTest extends ServiceUnitTest {

    @InjectMocks
    private FcmCacheInvalidationHandler fcmCacheInvalidationHandler;

    @Mock
    private FcmTokenCacheRepository fcmTokenCacheRepository;

    private static final Long TEST_USER_ID = 1L;

    @Nested
    @DisplayName("FCM 토큰 업데이트 이벤트 수신")
    class HandleUserFcmTokenUpdated {

        @Test
        void 유저가_참여중인_게임이_있다면_해당_게임의_FCM_캐시를_삭제한다() {
            // given
            UserFcmTokenUpdatedEvent event = new UserFcmTokenUpdatedEvent(TEST_USER_ID);
            User user = UserFixture.USER();
            setId(user, TEST_USER_ID);
            
            Game game = GameFixture.IN_PROGRESS_GAME();
            setId(game, 100L);
            
            GameParticipant participant = GameParticipantFixture.ALIVE_POLICE(game, user);

            given(gameParticipantRepository.findActiveParticipantByUserId(TEST_USER_ID))
                    .willReturn(Optional.of(participant));

            // when
            fcmCacheInvalidationHandler.handleUserFcmTokenUpdated(event);

            // then
            then(fcmTokenCacheRepository).should().deleteAllByGameId(game.getId());
        }

        @Test
        void 유저가_참여중인_게임이_없다면_캐시를_삭제하지_않고_넘어간다() {
            // given
            UserFcmTokenUpdatedEvent event = new UserFcmTokenUpdatedEvent(TEST_USER_ID);

            given(gameParticipantRepository.findActiveParticipantByUserId(TEST_USER_ID))
                    .willReturn(Optional.empty());

            // when
            fcmCacheInvalidationHandler.handleUserFcmTokenUpdated(event);

            // then
            then(fcmTokenCacheRepository).should(never()).deleteAllByGameId(any());
        }
    }
}
