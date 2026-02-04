package com.team.cops_and_robbers.play.lobby.application;

import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import com.team.cops_and_robbers.play.lobby.domain.LobbyEventType;
import com.team.cops_and_robbers.play.system.application.GameSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GameStartScheduleHandler {

    private final GameSchedulerService gameSchedulerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGameStartSchedule(LobbyEvent event) {
        if (event.type() == LobbyEventType.GAME_START) {
            gameSchedulerService.scheduleAllEvents(event.gameId());
        }
    }
}
