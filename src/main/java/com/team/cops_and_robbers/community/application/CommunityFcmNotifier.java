package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.fcm.FcmMessage;
import com.team.cops_and_robbers.common.fcm.FcmService;
import com.team.cops_and_robbers.community.application.dto.CommunityNotificationPush;
import com.team.cops_and_robbers.user.domain.UserDevice;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityFcmNotifier {

    private final FcmService fcmService;
    private final UserDeviceRepository userDeviceRepository;

    @Async("fcmExecutor")
    public CompletableFuture<Void> notifyCommentCreated(List<Long> userIds, CommunityNotificationPush push) {
        try {
            List<String> tokens = getCommunityTokens(userIds);
            if (tokens.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            FcmPayload payload = resolveCommentPayload(push);
            fcmService.send(new FcmMessage(tokens, payload.title(), payload.body(), payload.data()));
        } catch (Exception e) {
            log.error("[FCM] Async send failed | postId={}, type={}", push.communityPostId(), push.type(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private List<String> getCommunityTokens(List<Long> userIds) {
        return userDeviceRepository.findByUser_IdIn(userIds).stream()
                .filter(device -> device.getUser().isAllowCommunityPush())
                .map(UserDevice::getFcmToken)
                .filter(Objects::nonNull)
                .toList();
    }

    private FcmPayload resolveCommentPayload(CommunityNotificationPush push) {
        Map<String, String> data = Map.of(
                "type", push.type().name(),
                "postId", String.valueOf(push.communityPostId()));
        return new FcmPayload(push.postTitle(), push.content(), data);
    }

    private record FcmPayload(String title, String body, Map<String, String> data) {}
}
