package com.team.cops_and_robbers.play.lobby.application;

import com.team.cops_and_robbers.play.common.application.InGameParticipantCacheService;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEventType;
import com.team.cops_and_robbers.play.system.application.GameScheduleProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GameStartScheduleHandler {

    private final GameScheduleProducer gameScheduleProducer;
    private final InGameParticipantCacheService inGameParticipantCacheService;

    @Order(1)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGameStartSchedule(LobbyEvent event) {
        if (event.type() == LobbyEventType.GAME_START) {
            gameScheduleProducer.scheduleAllEvents(event.gameId());
            inGameParticipantCacheService.loadCache(event.gameId());
        }
    }
}
