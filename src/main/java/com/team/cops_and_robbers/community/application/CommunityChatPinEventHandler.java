package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.community.application.event.CommunityChatPinChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommunityChatPinEventHandler {

    private final CommunityChatPinPublisher communityChatPinPublisher;
    private final CommunityChatFcmNotifier communityChatFcmNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePinChanged(CommunityChatPinChangedEvent event) {
        communityChatPinPublisher.publish(event);
        communityChatFcmNotifier.notifyPinChanged(event);
    }
}
