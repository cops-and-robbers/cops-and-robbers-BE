package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.play.notification.application.GameFcmNotifier;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SystemEventHandler {

    private final SystemPublisher systemPublisher;
    private final GameSchedulerService gameSchedulerService;
    private final GameFcmNotifier gameFcmNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSystemEvent(SystemEvent event) {
        if (event.type() == SystemEventType.GAME_OVER) {
            gameSchedulerService.cancelSchedule(event.gameId());
        }
        systemPublisher.publish(event);
        gameFcmNotifier.notifySystemEvent(event);
    }
}
