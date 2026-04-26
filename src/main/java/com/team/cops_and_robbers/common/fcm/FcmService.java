package com.team.cops_and_robbers.common.fcm;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;

    public void send(FcmMessage fcmMessage) {
        List<String> validTokens = fcmMessage.tokens().stream()
                .filter(token -> token != null && !token.isBlank())
                .toList();
        if (validTokens.isEmpty()) return;

        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(validTokens)
                    .setNotification(Notification.builder()
                            .setTitle(fcmMessage.title())
                            .setBody(fcmMessage.body())
                            .build())
                    .putAllData(fcmMessage.data())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH).build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").build()).build())
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("[FCM] Multicast sent | success={}, failure={}",
                    response.getSuccessCount(), response.getFailureCount());

            logFailures(response, validTokens);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Multicast send failed | code={}, message={}",
                    e.getMessagingErrorCode(), e.getMessage());
        }
    }

    private void logFailures(BatchResponse response, List<String> tokens) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) continue;

            FirebaseMessagingException ex = sendResponse.getException();
            if (ex == null) continue;

            logTokenFailure(tokens.get(i), ex.getMessagingErrorCode());
        }
    }

    private void logTokenFailure(String token, MessagingErrorCode errorCode) {
        switch (errorCode) {
            case UNREGISTERED ->
                log.warn("[FCM] Token expired (app uninstalled or notifications blocked) | token={}", token);
            case INVALID_ARGUMENT ->
                log.warn("[FCM] Invalid token format | token={}", token);
            default ->
                log.error("[FCM] Send failed | code={}, token={}", errorCode, token);
        }
    }

}
