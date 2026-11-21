package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.play.notification.application.GameFcmNotifier;
import com.team.cops_and_robbers.play.common.application.InGameParticipantCacheService;
import com.team.cops_and_robbers.play.distance.application.ParticipantDistanceService;
import com.team.cops_and_robbers.play.location.application.RunnerLocationService;
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
    private final RunnerLocationService runnerLocationService;
    private final InGameParticipantCacheService inGameParticipantCacheService;
    private final ParticipantDistanceService participantDistanceService;

    private final GameFcmNotifier gameFcmNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSystemEvent(SystemEvent event) {
        systemPublisher.publish(event);
        if (event.type() == SystemEventType.GAME_OVER) {
            gameFcmNotifier.notifySystemEvent(event).whenComplete((r, ex) -> {
                runnerLocationService.clearRunnerLocations(event.gameId());
                runnerLocationService.clearRunnerLocationSnapshot(event.gameId());
                inGameParticipantCacheService.clearCache(event.gameId());
                participantDistanceService.clearDistances(event.gameId());
            });
        } else {
            gameFcmNotifier.notifySystemEvent(event);
        }
    }
}
