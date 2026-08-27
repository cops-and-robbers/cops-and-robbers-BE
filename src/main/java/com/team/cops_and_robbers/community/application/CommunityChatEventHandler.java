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
    private final CommunityChatFcmNotifier communityChatFcmNotifier;

    /** 메시지 저장은 이미 커밋된 뒤라 여기서 푸시를 보내도 알림함과 어긋나지 않는다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatMessageSaved(CommunityChatMessageSavedEvent event) {
        communityChatPublisher.publish(event.message());
        communityChatFcmNotifier.notifyMessageSent(event.message());
    }
}
