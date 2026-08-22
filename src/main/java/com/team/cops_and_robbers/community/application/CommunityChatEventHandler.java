package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.community.application.event.CommunityChatMessageSavedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommunityChatEventHandler {

    private final CommunityChatPublisher communityChatPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageSaved(CommunityChatMessageSavedEvent event) {
        communityChatPublisher.publish(event.message());
    }
}
