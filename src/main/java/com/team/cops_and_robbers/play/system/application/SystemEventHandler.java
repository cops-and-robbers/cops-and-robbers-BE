package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.play.common.application.InGameParticipantCacheService;
import com.team.cops_and_robbers.play.location.application.RobberLocationService;
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
    private final RobberLocationService robberLocationService;
    private final InGameParticipantCacheService inGameParticipantCacheService;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSystemEvent(SystemEvent event) {
        if (event.type() == SystemEventType.GAME_OVER) {
            robberLocationService.clearRobberLocations(event.gameId());
            inGameParticipantCacheService.clearCache(event.gameId());
        }
        systemPublisher.publish(event);
    }
}
