package com.team.cops_and_robbers.play.notification.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fcm.FcmMessage;
import com.team.cops_and_robbers.common.fcm.FcmService;
import com.team.cops_and_robbers.play.notification.repository.FcmTokenCacheRepository;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class GameFcmNotifierTest extends ServiceUnitTest {

    @InjectMocks
    private GameFcmNotifier gameFcmNotifier;

    @Mock
    private FcmService fcmService;

    @Mock
    private FcmTokenCacheRepository fcmTokenCacheRepository;

    private static final Long TEST_GAME_ID = 1L;
    private static final List<String> TOKENS = List.of("token1", "token2");

    @Nested
    @DisplayName("시스템 이벤트 알림 발송")
    class NotifySystemEvent {

        @Test
        void 캐시에_토큰이_있으면_DB_조회_없이_바로_알림을_발송한다() {
            // given (Cache Hit)
            SystemEvent event = SystemEvent.of(TEST_GAME_ID, SystemEventType.ARREST, null);
            given(fcmTokenCacheRepository.findGameTokens(TEST_GAME_ID)).willReturn(TOKENS);

            // when
            gameFcmNotifier.notifySystemEvent(event);

            // then
            then(fcmTokenCacheRepository).should().findGameTokens(TEST_GAME_ID);
            then(gameParticipantRepository).should(never()).findUserIdsByGameId(anyLong());
            then(fcmService).should().send(any(FcmMessage.class));
        }

        @Test
        void 캐시에_토큰이_없으면_DB를_조회하고_레디스에_저장한_뒤_알림을_발송한다() {
            // given (Cache Miss)
            SystemEvent event = SystemEvent.of(TEST_GAME_ID, SystemEventType.ARREST, null);
            List<Long> userIds = List.of(10L, 20L);
            
            given(fcmTokenCacheRepository.findGameTokens(TEST_GAME_ID)).willReturn(null);
            given(gameParticipantRepository.findUserIdsByGameId(TEST_GAME_ID)).willReturn(userIds);
            given(userDeviceRepository.findFcmTokensByUserIdsAndAllowPush(userIds)).willReturn(TOKENS);

            // when
            gameFcmNotifier.notifySystemEvent(event);

            // then
            then(fcmTokenCacheRepository).should().saveGameTokens(TEST_GAME_ID, TOKENS);
            then(fcmService).should().send(any(FcmMessage.class));
        }

        @Test
        void 모든_유저가_알림_거부_상태이면_FCM을_발송하지_않는다() {
            // given
            SystemEvent event = SystemEvent.of(TEST_GAME_ID, SystemEventType.ARREST, null);
            List<Long> userIds = List.of(10L, 20L);

            given(fcmTokenCacheRepository.findGameTokens(TEST_GAME_ID)).willReturn(null);
            given(gameParticipantRepository.findUserIdsByGameId(TEST_GAME_ID)).willReturn(userIds);
            given(userDeviceRepository.findFcmTokensByUserIdsAndAllowPush(userIds)).willReturn(List.of());

            // when
            gameFcmNotifier.notifySystemEvent(event);

            // then
            then(fcmTokenCacheRepository).should().saveGameTokens(TEST_GAME_ID, List.of());
            then(fcmService).should(never()).send(any(FcmMessage.class));
        }

        @Test
        void 이벤트_타입이_GAME_OVER이면_알림_발송_후_해당_게임의_캐시를_모두_삭제한다() {
            // given
            SystemEvent event = SystemEvent.of(TEST_GAME_ID, SystemEventType.GAME_OVER, null);
            given(fcmTokenCacheRepository.findGameTokens(TEST_GAME_ID)).willReturn(TOKENS);

            // when
            gameFcmNotifier.notifySystemEvent(event);

            // then
            then(fcmService).should().send(any(FcmMessage.class));
            then(fcmTokenCacheRepository).should().deleteAllByGameId(TEST_GAME_ID);
        }
    }
}
