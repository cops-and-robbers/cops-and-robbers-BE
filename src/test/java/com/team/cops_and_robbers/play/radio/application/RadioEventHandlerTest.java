package com.team.cops_and_robbers.play.radio.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.play.radio.infrastructure.RadioRoomClient;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class RadioEventHandlerTest extends ServiceUnitTest {

    @InjectMocks
    private RadioEventHandler radioEventHandler;

    @Mock
    private RadioRoomClient radioRoomClient;

    private static final Long TEST_GAME_ID = 1L;

    @Nested
    @DisplayName("시스템 이벤트 수신")
    class HandleSystemEvent {

        @Test
        void 게임이_끝나면_경찰_무전_방을_닫는다() {
            // when
            radioEventHandler.handleSystemEvent(SystemEvent.of(TEST_GAME_ID, SystemEventType.GAME_OVER, null));

            // then
            then(radioRoomClient).should().closeRoom("game-1-police");
        }

        @Test
        void 게임_종료가_아닌_이벤트는_무전_방을_닫지_않는다() {
            // when
            radioEventHandler.handleSystemEvent(SystemEvent.of(TEST_GAME_ID, SystemEventType.ARREST, null));

            // then
            then(radioRoomClient).should(never()).closeRoom(any());
        }
    }
}
