package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import com.team.cops_and_robbers.play.system.domain.SystemEventData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SystemEventHandler {

    private final SystemPublisher systemPublisher;
    private final GameSchedulerService gameSchedulerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSystemEvent(SystemEvent event) {
        switch (event.type()) {
            case GAME_OVER ->
                    gameSchedulerService.cancelSchedule(event.gameId());

            case ADDITIONAL_TIME_STARTED -> {
                SystemEventData.AdditionalTimeStartedData data =
                        (SystemEventData.AdditionalTimeStartedData) event.data();
                gameSchedulerService.cancelGameOverSchedule(event.gameId());
                gameSchedulerService.scheduleAdditionalTimeEvaluation(event.gameId(), data.trigger());
            }

            case GAME_RESUMED -> {
                SystemEventData.GameResumedData data =
                        (SystemEventData.GameResumedData) event.data();
                gameSchedulerService.scheduleGameOverAfterAdditionalTime(event.gameId(), data.remainingSeconds());
            }

            default -> {}
        }
        systemPublisher.publish(event);
    }
}
