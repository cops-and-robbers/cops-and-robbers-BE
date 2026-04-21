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
            log.info("[FCM 발송] 멀티캐스트 발송 완료 | 성공={}, 실패={}",
                    response.getSuccessCount(), response.getFailureCount());

            logFailures(response, validTokens);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM 발송 실패] 멀티캐스트 전송 중 오류 발생 | code={}, message={}",
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
                log.warn("[FCM 발송 실패] 만료된 토큰 (앱 삭제 또는 알림 차단) | token={}", token);
            case INVALID_ARGUMENT ->
                log.warn("[FCM 발송 실패] 잘못된 토큰 형식 | token={}", token);
            default ->
                log.error("[FCM 발송 실패] 전송 실패 | code={}, token={}", errorCode, token);
        }
    }

}
