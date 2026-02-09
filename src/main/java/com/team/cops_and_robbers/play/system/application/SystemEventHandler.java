package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SystemEventHandler {

    private final SystemPublisher systemPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSystemEvent(SystemEvent event) {
        systemPublisher.publish(event);
    }
}
