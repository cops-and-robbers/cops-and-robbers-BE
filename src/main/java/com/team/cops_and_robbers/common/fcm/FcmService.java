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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;

    public void send(FcmMessage fcmMessage) {
        List<String> validTokens = fcmMessage.tokens().stream()
                .filter(StringUtils::hasText)
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

            if (response.getFailureCount() > 0) {
                logFailures(response, validTokens);
            }
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Multicast send failed | code={}, message={}",
                    e.getMessagingErrorCode(), e.getMessage());
        }
    }

    private void logFailures(BatchResponse response, List<String> tokens) {
        List<SendResponse> responses = response.getResponses();
        Map<MessagingErrorCode, List<String>> failures = new EnumMap<>(MessagingErrorCode.class);

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful() || sendResponse.getException() == null) continue;

            MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();
            failures.computeIfAbsent(errorCode, key -> new ArrayList<>()).add(tokens.get(i));
        }

        failures.forEach(this::logFailureSummary);
    }

    private void logFailureSummary(MessagingErrorCode errorCode, List<String> failedTokens) {
        switch (errorCode) {
            case UNREGISTERED ->
                log.warn("[FCM] Token expired (app uninstalled or notifications blocked) | count={}, tokens={}",
                        failedTokens.size(), failedTokens);
            case INVALID_ARGUMENT ->
                log.warn("[FCM] Invalid token format | count={}, tokens={}",
                        failedTokens.size(), failedTokens);
            default ->
                log.error("[FCM] Send failed | code={}, count={}, tokens={}",
                        errorCode, failedTokens.size(), failedTokens);
        }
    }

}
