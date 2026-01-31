package com.team.cops_and_robbers.play.lobby.application;

import com.team.cops_and_robbers.play.lobby.domain.LobbyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LobbyEventHandler {

    private final LobbyPublisher lobbyPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLobbyEvent(LobbyEvent event) {
        lobbyPublisher.publish(event);
    }
}
