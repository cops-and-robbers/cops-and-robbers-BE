package com.team.cops_and_robbers.play.radio.application;

import com.team.cops_and_robbers.play.radio.domain.RadioRoom;
import com.team.cops_and_robbers.play.radio.infrastructure.RadioRoomClient;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RadioEventHandler {

    private final RadioRoomClient radioRoomClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSystemEvent(SystemEvent event) {
        if (event.type() != SystemEventType.GAME_OVER) {
            return;
        }
        radioRoomClient.closeRoom(RadioRoom.policeRoomOf(event.gameId()));
    }
}
