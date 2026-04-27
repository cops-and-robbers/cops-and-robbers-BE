package com.team.cops_and_robbers.play.notification.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.fcm.FcmMessage;
import com.team.cops_and_robbers.common.fcm.FcmService;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.play.common.domain.InGameParticipantCache;
import com.team.cops_and_robbers.play.common.repository.InGameParticipantCacheRepository;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class GameFcmNotifierTest extends ServiceUnitTest {

    @InjectMocks
    private GameFcmNotifier gameFcmNotifier;

    @Mock
    private FcmService fcmService;

    @Mock
    private InGameParticipantCacheRepository inGameParticipantCacheRepository;

    private static final Long TEST_GAME_ID = 1L;

    @Nested
    @DisplayName("시스템 이벤트 알림 발송")
    class NotifySystemEvent {

        @Test
        void 캐시에_토큰이_있으면_DB_조회_없이_바로_알림을_발송한다() {
            // given
            SystemEvent event = SystemEvent.of(TEST_GAME_ID, SystemEventType.ARREST, null);
            List<InGameParticipantCache> caches = List.of(
                    new InGameParticipantCache("nick1", Team.POLICE, "token1"),
                    new InGameParticipantCache("nick2", Team.ROBBER, "token2")
            );
            given(inGameParticipantCacheRepository.findAllByGameId(TEST_GAME_ID)).willReturn(caches);

            // when
            gameFcmNotifier.notifySystemEvent(event);

            // then
            then(fcmService).should().send(any(FcmMessage.class));
        }

        @Test
        void 모든_유저가_알림_거부_상태이면_FCM을_발송하지_않는다() {
            // given
            SystemEvent event = SystemEvent.of(TEST_GAME_ID, SystemEventType.ARREST, null);
            List<InGameParticipantCache> caches = List.of(
                    new InGameParticipantCache("nick1", Team.POLICE, null),
                    new InGameParticipantCache("nick2", Team.ROBBER, null)
            );
            given(inGameParticipantCacheRepository.findAllByGameId(TEST_GAME_ID)).willReturn(caches);

            // when
            gameFcmNotifier.notifySystemEvent(event);

            // then
            then(fcmService).should(never()).send(any(FcmMessage.class));
        }
    }
}
