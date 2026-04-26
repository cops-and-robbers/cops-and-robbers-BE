package com.team.cops_and_robbers.play.notification.application;

import com.team.cops_and_robbers.auth.application.event.UserFcmTokenUpdatedEvent;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.notification.repository.FcmTokenCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmCacheInvalidationHandler {

    private final GameParticipantRepository gameParticipantRepository;
    private final FcmTokenCacheRepository fcmTokenCacheRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserFcmTokenUpdated(UserFcmTokenUpdatedEvent event) {
        Long userId = event.userId();

        gameParticipantRepository.findActiveParticipantByUserId(userId)
                .ifPresent(participant -> {
                    Long gameId = participant.getGame().getId();
                    fcmTokenCacheRepository.deleteAllByGameId(gameId);
                    log.info("[FCM] Token cache invalidated on login | userId={}, gameId={}", userId, gameId);
                });
    }
}
